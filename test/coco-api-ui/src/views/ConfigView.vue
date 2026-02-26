<template>
  <div class="config-view">
    <div class="view-header fluent-card">
      <div class="header-content">
        <h1 class="view-title">标签过滤配置</h1>
        <div class="header-actions">
          <el-button
            v-if="!configStore.config"
            type="primary"
            :icon="Plus"
            :loading="configStore.loading"
            @click="handleCreate"
          >
            创建配置
          </el-button>
          <el-button
            v-else
            type="primary"
            :icon="Check"
            :loading="saving"
            @click="handleSave"
          >
            保存配置
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="configStore.config" class="view-content fluent-card">
      <el-form :model="formData" label-width="120px" label-position="left">
        <!-- Enabled Switch -->
        <el-form-item label="启用状态">
          <el-switch
            v-model="formData.enabled"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>

        <!-- Match Mode -->
        <el-form-item label="匹配模式">
          <el-radio-group v-model="formData.matchMode">
            <el-radio value="whitelist">白名单</el-radio>
            <el-radio value="blacklist">黑名单</el-radio>
          </el-radio-group>
          <div class="form-item-tip">
            白名单：仅保留匹配的标签；黑名单：过滤掉匹配的标签
          </div>
        </el-form-item>

        <!-- Author Tags -->
        <el-form-item label="作者标签">
          <el-select
            v-model="formData.authorIds"
            multiple
            filterable
            placeholder="选择作者"
            style="width: 100%"
            :loading="authorStore.loading"
          >
            <el-option
              v-for="author in allAuthors"
              :key="author.id"
              :label="getAuthorLabel(author)"
              :value="author.id"
            >
              <div class="select-option-content">
                <span class="option-name">{{ author.name }}</span>
                <span v-if="author.aliases.length > 0" class="option-aliases">
                  ({{ author.aliases.join(', ') }})
                </span>
              </div>
            </el-option>
          </el-select>
          <div class="form-item-tip">
            已选择 {{ formData.authorIds.length }} 个作者
          </div>
        </el-form-item>

        <!-- Character Tags -->
        <el-form-item label="角色标签">
          <el-select
            v-model="formData.characterIds"
            multiple
            filterable
            placeholder="选择角色"
            style="width: 100%"
            :loading="characterStore.loading"
          >
            <el-option
              v-for="character in allCharacters"
              :key="character.id"
              :label="getCharacterLabel(character)"
              :value="character.id"
            >
              <div class="select-option-content">
                <span class="option-name">{{ character.name }}</span>
                <span v-if="character.aliases.length > 0" class="option-aliases">
                  ({{ character.aliases.join(', ') }})
                </span>
                <span v-if="character.workName" class="option-work">
                  - {{ character.workName }}
                </span>
              </div>
            </el-option>
          </el-select>
          <div class="form-item-tip">
            已选择 {{ formData.characterIds.length }} 个角色
          </div>
        </el-form-item>

        <!-- Work Tags -->
        <el-form-item label="原作标签">
          <el-select
            v-model="formData.workIds"
            multiple
            filterable
            placeholder="选择原作"
            style="width: 100%"
            :loading="workStore.loading"
          >
            <el-option
              v-for="work in allWorks"
              :key="work.id"
              :label="getWorkLabel(work)"
              :value="work.id"
            >
              <div class="select-option-content">
                <span class="option-name">{{ work.name }}</span>
                <span v-if="work.aliases.length > 0" class="option-aliases">
                  ({{ work.aliases.join(', ') }})
                </span>
              </div>
            </el-option>
          </el-select>
          <div class="form-item-tip">
            已选择 {{ formData.workIds.length }} 个原作
          </div>
        </el-form-item>

        <!-- Custom Tags -->
        <el-form-item label="自定义标签">
          <div class="custom-tags-editor">
            <div
              v-for="(_value, key, index) in formData.customTags"
              :key="index"
              class="custom-tag-item"
            >
              <el-input
                v-model="customTagKeys[index]"
                placeholder="键"
                style="width: 200px"
                @blur="handleCustomTagKeyChange(index, key)"
              />
              <el-input
                v-model="formData.customTags[key]"
                placeholder="值"
                style="width: 300px; margin-left: 8px"
              />
              <el-button
                :icon="Delete"
                type="danger"
                text
                @click="handleRemoveCustomTag(key)"
              />
            </div>
            <el-button
              :icon="Plus"
              type="primary"
              text
              @click="handleAddCustomTag"
            >
              添加自定义标签
            </el-button>
          </div>
          <div class="form-item-tip">
            自定义标签键值对，用于匹配特定的标签
          </div>
        </el-form-item>

        <!-- Expand Tags Test -->
        <el-form-item label="标签展开测试">
          <el-button
            :icon="View"
            :loading="expandLoading"
            @click="handleExpandTest"
          >
            测试展开
          </el-button>
          <div v-if="expandedTags.length > 0" class="expanded-tags-result">
            <div class="result-title">展开后的标签列表（{{ expandedTags.length }} 个）：</div>
            <div class="tags-container">
              <el-tag
                v-for="(tag, index) in expandedTags"
                :key="index"
                size="small"
                style="margin: 4px"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- Empty State -->
    <div v-else-if="!configStore.loading" class="view-content fluent-card">
      <el-empty description="暂无配置">
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          创建配置
        </el-button>
      </el-empty>
    </div>

    <!-- Loading State -->
    <div v-else class="view-content fluent-card">
      <el-skeleton :rows="8" animated />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Check, Delete, View } from '@element-plus/icons-vue'
import { useConfigStore } from '@/stores/config'
import { useAuthorStore } from '@/stores/author'
import { useWorkStore } from '@/stores/work'
import { useCharacterStore } from '@/stores/character'
import type { TagFilterConfigCreateDTO } from '@/types/models'
import type { Author, Work, Character } from '@/types/models'

const configStore = useConfigStore()
const authorStore = useAuthorStore()
const workStore = useWorkStore()
const characterStore = useCharacterStore()

const saving = ref(false)
const expandLoading = ref(false)
const expandedTags = ref<string[]>([])

// All data for selectors
const allAuthors = ref<Author[]>([])
const allWorks = ref<Work[]>([])
const allCharacters = ref<Character[]>([])

// Form data
const formData = reactive<TagFilterConfigCreateDTO>({
  authorIds: [],
  characterIds: [],
  workIds: [],
  customTags: {},
  matchMode: 'whitelist',
  enabled: true
})

// Custom tags keys array for v-model binding
const customTagKeys = ref<string[]>([])

// Load config and all data
onMounted(async () => {
  await loadConfig()
  await loadAllData()
})

// Watch config changes to update form
watch(() => configStore.config, (newConfig) => {
  if (newConfig) {
    formData.authorIds = [...newConfig.authorIds]
    formData.characterIds = [...newConfig.characterIds]
    formData.workIds = [...newConfig.workIds]
    formData.customTags = { ...newConfig.customTags }
    formData.matchMode = newConfig.matchMode
    formData.enabled = newConfig.enabled
    
    // Update custom tag keys
    customTagKeys.value = Object.keys(formData.customTags)
  }
}, { immediate: true })

async function loadConfig() {
  try {
    await configStore.fetchGlobal()
  } catch (error: any) {
    console.error('Failed to load config:', error)
  }
}

async function loadAllData() {
  try {
    // Load all authors (fetch large page)
    const authorResponse = await authorStore.fetchPage({ current: 1, size: 1000 })
    allAuthors.value = authorResponse.records

    // Load all works
    const workResponse = await workStore.fetchPage({ current: 1, size: 1000 })
    allWorks.value = workResponse.records

    // Load all characters
    const characterResponse = await characterStore.fetchPage({ current: 1, size: 1000 })
    allCharacters.value = characterResponse.records
  } catch (error: any) {
    ElMessage.error('加载数据失败')
    console.error('Failed to load data:', error)
  }
}

function getAuthorLabel(author: Author): string {
  if (author.aliases.length > 0) {
    return `${author.name} (${author.aliases.join(', ')})`
  }
  return author.name
}

function getWorkLabel(work: Work): string {
  if (work.aliases.length > 0) {
    return `${work.name} (${work.aliases.join(', ')})`
  }
  return work.name
}

function getCharacterLabel(character: Character): string {
  let label = character.name
  if (character.aliases.length > 0) {
    label += ` (${character.aliases.join(', ')})`
  }
  if (character.workName) {
    label += ` - ${character.workName}`
  }
  return label
}

function handleAddCustomTag() {
  const newKey = `key_${Date.now()}`
  formData.customTags[newKey] = ''
  customTagKeys.value.push(newKey)
}

function handleRemoveCustomTag(key: string) {
  delete formData.customTags[key]
  const index = customTagKeys.value.indexOf(key)
  if (index > -1) {
    customTagKeys.value.splice(index, 1)
  }
}

function handleCustomTagKeyChange(index: number, oldKey: string) {
  const newKey = customTagKeys.value[index]
  if (newKey !== oldKey && newKey) {
    const oldValue = formData.customTags[oldKey]
    delete formData.customTags[oldKey]
    formData.customTags[newKey] = oldValue
  }
}

async function handleCreate() {
  try {
    saving.value = true
    await configStore.createOrUpdate(formData)
    ElMessage.success('配置创建成功')
  } catch (error: any) {
    ElMessage.error(error.message || '创建配置失败')
  } finally {
    saving.value = false
  }
}

async function handleSave() {
  if (!configStore.config) return

  try {
    saving.value = true
    await configStore.update(configStore.config.id, formData)
    ElMessage.success('配置保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

async function handleExpandTest() {
  try {
    expandLoading.value = true
    const tags = await configStore.expandTags(formData)
    expandedTags.value = tags
    ElMessage.success(`展开成功，共 ${tags.length} 个标签`)
  } catch (error: any) {
    ElMessage.error(error.message || '展开测试失败')
    expandedTags.value = []
  } finally {
    expandLoading.value = false
  }
}
</script>

<style scoped>
.config-view {
  padding: var(--spacing-lg);
  min-height: 100vh;
}

.view-header {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-lg);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.view-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.view-content {
  padding: var(--spacing-lg);
}

.form-item-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.select-option-content {
  display: flex;
  align-items: center;
  gap: 4px;
}

.option-name {
  font-weight: 500;
}

.option-aliases {
  color: var(--text-secondary);
  font-size: 12px;
}

.option-work {
  color: var(--text-tertiary);
  font-size: 12px;
}

.custom-tags-editor {
  width: 100%;
}

.custom-tag-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.expanded-tags-result {
  margin-top: 16px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
}

.result-title {
  margin-bottom: 12px;
  font-weight: 500;
  color: var(--text-primary);
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* Responsive */
@media (max-width: 768px) {
  .config-view {
    padding: var(--spacing-md);
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .custom-tag-item {
    flex-direction: column;
    align-items: stretch;
  }

  .custom-tag-item .el-input {
    width: 100% !important;
    margin-left: 0 !important;
    margin-bottom: 8px;
  }
}
</style>
