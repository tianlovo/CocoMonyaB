import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import fc from 'fast-check'
import AuthorDialog from './AuthorDialog.vue'
import { ElDialog, ElForm, ElFormItem, ElInput, ElButton } from 'element-plus'

/**
 * 属性测试：动态列表项管理
 * 
 * 这些测试验证动态列表（别名、网址）的添加和删除操作在各种输入下的正确性
 */

describe('AuthorDialog Property Tests', () => {
  /**
   * Feature: tag-management-frontend, Property 6: 动态列表项管理
   * **Validates: Requirements 4.10, 4.11**
   * 
   * 对于任何支持动态添加/删除的列表字段（别名、网址），添加操作应增加列表长度，删除操作应减少列表长度
   */
  describe('Property 6: 动态列表项管理', () => {
    it('should increase aliases list length when adding an alias', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 10 }), // Initial number of aliases
          (initialCount) => {
            const wrapper = mount(AuthorDialog, {
              props: {
                visible: true,
                author: null
              },
              global: {
                components: {
                  ElDialog,
                  ElForm,
                  ElFormItem,
                  ElInput,
                  ElButton
                },
                stubs: {
                  teleport: true
                }
              }
            })

            // Set initial aliases
            const formData = (wrapper.vm as any).formData
            formData.aliases = Array(initialCount).fill('')
            
            const initialLength = formData.aliases.length
            expect(initialLength).toBe(initialCount)

            // Add an alias
            ;(wrapper.vm as any).addAlias()

            // Verify length increased by 1
            const newLength = formData.aliases.length
            expect(newLength).toBe(initialLength + 1)
            expect(newLength).toBe(initialCount + 1)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should decrease aliases list length when removing an alias', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 10 }), // Initial number of aliases (at least 1 to remove)
          fc.integer({ min: 0, max: 9 }), // Index to remove
          (initialCount, removeIndexBase) => {
            const wrapper = mount(AuthorDialog, {
              props: {
                visible: true,
                author: null
              },
              global: {
                components: {
                  ElDialog,
                  ElForm,
                  ElFormItem,
                  ElInput,
                  ElButton
                },
                stubs: {
                  teleport: true
                }
              }
            })

            // Set initial aliases
            const formData = (wrapper.vm as any).formData
            formData.aliases = Array(initialCount).fill('').map((_, i) => `alias${i}`)
            
            const initialLength = formData.aliases.length
            expect(initialLength).toBe(initialCount)

            // Calculate valid remove index
            const removeIndex = removeIndexBase % initialCount

            // Remove an alias
            ;(wrapper.vm as any).removeAlias(removeIndex)

            // Verify length decreased by 1
            const newLength = formData.aliases.length
            expect(newLength).toBe(initialLength - 1)
            expect(newLength).toBe(initialCount - 1)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should increase urls list length when adding a url', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 10 }), // Initial number of urls
          (initialCount) => {
            const wrapper = mount(AuthorDialog, {
              props: {
                visible: true,
                author: null
              },
              global: {
                components: {
                  ElDialog,
                  ElForm,
                  ElFormItem,
                  ElInput,
                  ElButton
                },
                stubs: {
                  teleport: true
                }
              }
            })

            // Set initial urls
            const formData = (wrapper.vm as any).formData
            formData.urls = Array(initialCount).fill('')
            
            const initialLength = formData.urls.length
            expect(initialLength).toBe(initialCount)

            // Add a url
            ;(wrapper.vm as any).addUrl()

            // Verify length increased by 1
            const newLength = formData.urls.length
            expect(newLength).toBe(initialLength + 1)
            expect(newLength).toBe(initialCount + 1)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should decrease urls list length when removing a url', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 10 }), // Initial number of urls (at least 1 to remove)
          fc.integer({ min: 0, max: 9 }), // Index to remove
          (initialCount, removeIndexBase) => {
            const wrapper = mount(AuthorDialog, {
              props: {
                visible: true,
                author: null
              },
              global: {
                components: {
                  ElDialog,
                  ElForm,
                  ElFormItem,
                  ElInput,
                  ElButton
                },
                stubs: {
                  teleport: true
                }
              }
            })

            // Set initial urls
            const formData = (wrapper.vm as any).formData
            formData.urls = Array(initialCount).fill('').map((_, i) => `https://example${i}.com`)
            
            const initialLength = formData.urls.length
            expect(initialLength).toBe(initialCount)

            // Calculate valid remove index
            const removeIndex = removeIndexBase % initialCount

            // Remove a url
            ;(wrapper.vm as any).removeUrl(removeIndex)

            // Verify length decreased by 1
            const newLength = formData.urls.length
            expect(newLength).toBe(initialLength - 1)
            expect(newLength).toBe(initialCount - 1)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should maintain list integrity after multiple add and remove operations', () => {
      fc.assert(
        fc.property(
          fc.array(
            fc.record({
              operation: fc.constantFrom('add', 'remove'),
              listType: fc.constantFrom('aliases', 'urls')
            }),
            { minLength: 5, maxLength: 20 }
          ),
          (operations) => {
            const wrapper = mount(AuthorDialog, {
              props: {
                visible: true,
                author: null
              },
              global: {
                components: {
                  ElDialog,
                  ElForm,
                  ElFormItem,
                  ElInput,
                  ElButton
                },
                stubs: {
                  teleport: true
                }
              }
            })

            const formData = (wrapper.vm as any).formData
            let expectedAliasesLength = 0
            let expectedUrlsLength = 0

            // Execute operations
            for (const op of operations) {
              if (op.listType === 'aliases') {
                if (op.operation === 'add') {
                  ;(wrapper.vm as any).addAlias()
                  expectedAliasesLength++
                } else if (op.operation === 'remove' && formData.aliases.length > 0) {
                  const removeIndex = Math.floor(Math.random() * formData.aliases.length)
                  ;(wrapper.vm as any).removeAlias(removeIndex)
                  expectedAliasesLength--
                }
              } else {
                if (op.operation === 'add') {
                  ;(wrapper.vm as any).addUrl()
                  expectedUrlsLength++
                } else if (op.operation === 'remove' && formData.urls.length > 0) {
                  const removeIndex = Math.floor(Math.random() * formData.urls.length)
                  ;(wrapper.vm as any).removeUrl(removeIndex)
                  expectedUrlsLength--
                }
              }
            }

            // Verify final lengths match expected
            expect(formData.aliases.length).toBe(expectedAliasesLength)
            expect(formData.urls.length).toBe(expectedUrlsLength)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should correctly remove the specified item by index', () => {
      fc.assert(
        fc.property(
          fc.array(fc.string({ minLength: 1, maxLength: 20 }), { minLength: 3, maxLength: 10 }),
          fc.integer({ min: 0, max: 9 }),
          (initialAliases, removeIndexBase) => {
            const wrapper = mount(AuthorDialog, {
              props: {
                visible: true,
                author: null
              },
              global: {
                components: {
                  ElDialog,
                  ElForm,
                  ElFormItem,
                  ElInput,
                  ElButton
                },
                stubs: {
                  teleport: true
                }
              }
            })

            // Set initial aliases
            const formData = (wrapper.vm as any).formData
            formData.aliases = [...initialAliases]
            
            const removeIndex = removeIndexBase % initialAliases.length
            const itemToRemove = initialAliases[removeIndex]

            // Remove the alias
            ;(wrapper.vm as any).removeAlias(removeIndex)

            // Verify the correct item was removed
            expect(formData.aliases).not.toContain(itemToRemove)
            expect(formData.aliases.length).toBe(initialAliases.length - 1)
            
            // Verify remaining items are correct
            const expectedRemaining = [
              ...initialAliases.slice(0, removeIndex),
              ...initialAliases.slice(removeIndex + 1)
            ]
            expect(formData.aliases).toEqual(expectedRemaining)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should handle empty list operations correctly', () => {
      fc.assert(
        fc.property(
          fc.constant(true),
          () => {
            const wrapper = mount(AuthorDialog, {
              props: {
                visible: true,
                author: null
              },
              global: {
                components: {
                  ElDialog,
                  ElForm,
                  ElFormItem,
                  ElInput,
                  ElButton
                },
                stubs: {
                  teleport: true
                }
              }
            })

            const formData = (wrapper.vm as any).formData
            
            // Verify initial state is empty
            expect(formData.aliases.length).toBe(0)
            expect(formData.urls.length).toBe(0)

            // Add items to empty lists
            ;(wrapper.vm as any).addAlias()
            expect(formData.aliases.length).toBe(1)

            ;(wrapper.vm as any).addUrl()
            expect(formData.urls.length).toBe(1)

            // Remove items to make lists empty again
            ;(wrapper.vm as any).removeAlias(0)
            expect(formData.aliases.length).toBe(0)

            ;(wrapper.vm as any).removeUrl(0)
            expect(formData.urls.length).toBe(0)
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
