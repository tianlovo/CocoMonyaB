package org.xlyo.cocomonyab.service.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterQueryDTO;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.TagFilterConfig;
import org.xlyo.cocomonyab.domain.entity.tag.Work;
import org.xlyo.cocomonyab.domain.vo.tag.CharacterVO;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.TagFilterConfigRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CharacterService的基于属性的测试
 * 测试设计文档中的属性16、17、19、32
 * 
 * 注意：使用JUnit @Test手动生成属性而不是jqwik，
 * 因为jqwik不能很好地支持Spring的依赖注入
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya_test"
})
class CharacterServicePropertyTest {
    
    @Autowired
    private CharacterService characterService;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private WorkRepository workRepository;
    
    @Autowired
    private TagFilterConfigRepository tagFilterConfigRepository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // 清理所有现有的测试数据
        characterRepository.deleteAll();
        workRepository.deleteAll();
        tagFilterConfigRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // 测试后清理数据
        characterRepository.deleteAll();
        workRepository.deleteAll();
        tagFilterConfigRepository.deleteAll();
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 16: 角色原作引用有效性
     * 
     * 对于任何角色创建或更新操作，如果指定的原作ID在原作库中不存在，
     * 则操作应该被拒绝并返回引用错误。
     * 
     * **Validates: Requirements 3.5**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-16")
    void characterWorkReferenceValidity() {
        // 使用随机有效输入运行100次迭代
        for (int i = 0; i < 100; i++) {
            // 测试1：使用不存在的原作ID创建角色应该失败
            String nonExistentWorkId = "nonexistent_" + random.nextInt(100000);
            CharacterCreateDTO createDTO = new CharacterCreateDTO();
            createDTO.setName("Character_" + random.nextInt(100000));
            createDTO.setAliases(new ArrayList<>());
            createDTO.setWorkId(nonExistentWorkId);
            
            assertThrows(BusinessException.class, () -> {
                characterService.create(createDTO);
            }, "Creating character with non-existent work ID should fail (iteration " + i + ")");
            
            // 测试2：使用有效的原作ID创建角色应该成功
            String workName = "Work_" + random.nextInt(100000);
            Work work = createWork(workName, List.of());
            work = workRepository.save(work);
            
            createDTO.setWorkId(work.getId());
            CharacterVO createdCharacter = assertDoesNotThrow(() -> {
                return characterService.create(createDTO);
            }, "Creating character with valid work ID should succeed (iteration " + i + ")");
            
            assertNotNull(createdCharacter);
            assertEquals(work.getId(), createdCharacter.getWorkId());
            
            // 清理数据
            characterRepository.deleteById(createdCharacter.getId());
            workRepository.deleteById(work.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 17: 角色删除引用完整性检查
     * 
     * 对于任何角色，如果该角色在过滤配置的角色标签中被引用，
     * 则删除操作应该被拒绝并返回引用信息；如果使用强制删除选项，则所有引用应该被自动清理。
     * 
     * **Validates: Requirements 3.7, 10.5, 10.6, 10.7**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-17")
    void characterDeleteReferenceIntegrityCheck() {
        // 使用随机有效输入运行100次迭代
        for (int i = 0; i < 100; i++) {
            // 首先创建一个原作
            String workName = "Work_" + random.nextInt(100000);
            Work work = createWork(workName, List.of());
            work = workRepository.save(work);
            
            // 创建一个角色
            String characterName = "Character_" + random.nextInt(100000);
            Character character = createCharacter(characterName, List.of(), work.getId(), null);
            character = characterRepository.save(character);
            
            // 创建一个引用该角色的过滤配置
            TagFilterConfig config = createFilterConfig(List.of(), List.of(character.getId()), List.of());
            config = tagFilterConfigRepository.save(config);
            
            // 测试1：当存在引用时，不使用强制删除应该失败
            String characterId = character.getId();
            assertThrows(BusinessException.class, () -> {
                characterService.delete(characterId, false);
            }, "Deleting character with references should fail without force (iteration " + i + ")");
            
            // 测试2：使用强制删除应该成功
            assertDoesNotThrow(() -> {
                characterService.delete(characterId, true);
            }, "Deleting character with force should succeed (iteration " + i + ")");
            
            // 验证角色已被删除
            assertFalse(characterRepository.existsById(characterId),
                "Character should be deleted (iteration " + i + ")");
            
            // 清理数据
            tagFilterConfigRepository.deleteById(config.getId());
            workRepository.deleteById(work.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 19: 角色分页查询正确性
     * 
     * 对于任何角色集合，分页查询的所有页面合并后应该包含所有角色，且没有重复或遗漏。
     * 
     * **Validates: Requirements 3.9**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-19")
    void characterPaginationCorrectness() {
        // 使用随机有效输入运行100次迭代
        for (int i = 0; i < 100; i++) {
            // 首先创建一个原作
            String workName = "Work_" + random.nextInt(100000);
            Work work = createWork(workName, List.of());
            work = workRepository.save(work);
            
            // 创建随机数量的角色（5到20个之间）
            int characterCount = 5 + random.nextInt(16);
            List<String> createdCharacterIds = new ArrayList<>();
            
            for (int j = 0; j < characterCount; j++) {
                String name = "Character_" + i + "_" + j + "_" + random.nextInt(100000);
                Character character = createCharacter(name, List.of(), work.getId(), null);
                character = characterRepository.save(character);
                createdCharacterIds.add(character.getId());
            }
            
            // 使用页大小为3查询所有页面
            long pageSize = 3L;
            Set<String> retrievedCharacterIds = new HashSet<>();
            long currentPage = 1L;
            
            while (true) {
                PageResponse<CharacterVO> response = characterService.page(currentPage, pageSize, new CharacterQueryDTO());
                
                // 从当前页收集角色ID
                for (CharacterVO vo : response.getData().getRecords()) {
                    retrievedCharacterIds.add(vo.getId());
                }
                
                // 检查是否已到达最后一页
                if (currentPage >= response.getData().getPages()) {
                    break;
                }
                currentPage++;
            }
            
            // 验证所有创建的角色都被检索到
            assertEquals(createdCharacterIds.size(), retrievedCharacterIds.size(),
                "All characters should be retrieved (iteration " + i + ")");
            
            for (String createdId : createdCharacterIds) {
                assertTrue(retrievedCharacterIds.contains(createdId),
                    "Character " + createdId + " should be in retrieved results (iteration " + i + ")");
            }
            
            // 清理数据
            characterRepository.deleteAll();
            workRepository.deleteById(work.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 32: 角色种族过滤正确性
     * 
     * 对于任何种族值，按该种族过滤角色应该返回所有种族字段等于该值的角色，
     * 且不包含其他种族的角色。
     * 
     * **Validates: Requirements 11.4**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-32")
    void characterSpeciesFilterCorrectness() {
        // 使用随机有效输入运行100次迭代
        for (int i = 0; i < 100; i++) {
            // 首先创建一个原作
            String workName = "Work_" + random.nextInt(100000);
            Work work = createWork(workName, List.of());
            work = workRepository.save(work);
            
            // 定义测试种族
            String targetSpecies = "Species_" + random.nextInt(1000);
            String otherSpecies = "OtherSpecies_" + random.nextInt(1000);
            
            // 创建目标种族的角色
            int targetCount = 3 + random.nextInt(5);
            List<String> targetCharacterIds = new ArrayList<>();
            for (int j = 0; j < targetCount; j++) {
                String name = "TargetChar_" + i + "_" + j + "_" + random.nextInt(100000);
                Character character = createCharacter(name, List.of(), work.getId(), targetSpecies);
                character = characterRepository.save(character);
                targetCharacterIds.add(character.getId());
            }
            
            // 创建其他种族的角色
            int otherCount = 2 + random.nextInt(4);
            List<String> otherCharacterIds = new ArrayList<>();
            for (int j = 0; j < otherCount; j++) {
                String name = "OtherChar_" + i + "_" + j + "_" + random.nextInt(100000);
                Character character = createCharacter(name, List.of(), work.getId(), otherSpecies);
                character = characterRepository.save(character);
                otherCharacterIds.add(character.getId());
            }
            
            // 使用种族过滤器查询
            CharacterQueryDTO query = new CharacterQueryDTO();
            query.setSpecies(targetSpecies);
            
            PageResponse<CharacterVO> response = characterService.page(1L, 100L, query);
            List<CharacterVO> results = response.getData().getRecords();
            
            // 验证所有结果都具有目标种族
            for (CharacterVO vo : results) {
                assertEquals(targetSpecies, vo.getSpecies(),
                    "All results should have target species (iteration " + i + ")");
            }
            
            // 验证所有目标角色都在结果中
            Set<String> resultIds = new HashSet<>();
            for (CharacterVO vo : results) {
                resultIds.add(vo.getId());
            }
            
            for (String targetId : targetCharacterIds) {
                assertTrue(resultIds.contains(targetId),
                    "Target character " + targetId + " should be in results (iteration " + i + ")");
            }
            
            // 验证其他种族的角色不在结果中
            for (String otherId : otherCharacterIds) {
                assertFalse(resultIds.contains(otherId),
                    "Other species character " + otherId + " should not be in results (iteration " + i + ")");
            }
            
            // 清理数据
            characterRepository.deleteAll();
            workRepository.deleteById(work.getId());
        }
    }
    
    // 辅助方法
    
    private Work createWork(String name, List<String> aliases) {
        Work work = new Work();
        work.setName(name);
        work.setAliases(new ArrayList<>(aliases));
        work.setUrls(new ArrayList<>());
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        return work;
    }
    
    private Character createCharacter(String name, List<String> aliases, String workId, String species) {
        Character character = new Character();
        character.setName(name);
        character.setAliases(new ArrayList<>(aliases));
        character.setWorkId(workId);
        character.setSpecies(species);
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());
        return character;
    }
    
    private TagFilterConfig createFilterConfig(List<String> authorIds, List<String> characterIds, List<String> workIds) {
        TagFilterConfig config = new TagFilterConfig();
        config.setAuthorIds(new ArrayList<>(authorIds));
        config.setCharacterIds(new ArrayList<>(characterIds));
        config.setWorkIds(new ArrayList<>(workIds));
        config.setCustomTags(new HashMap<>());
        config.setMatchMode("whitelist");
        config.setEnabled(true);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return config;
    }
}
