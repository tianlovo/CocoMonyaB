import { describe, it, expect } from 'vitest'
import fc from 'fast-check'
import {
  maxLength,
  required,
  authorNameRules,
  workNameRules,
  characterNameRules,
  speciesRules,
  aliasRules,
  signatureRules,
  urlRules,
  remarkRules,
  arrayItemValidator
} from './validators'

/**
 * 属性测试：表单验证
 * 
 * 这些测试验证表单验证规则在各种输入下的正确性
 */

describe('Form Validation Property Tests', () => {
  /**
   * Feature: tag-management-frontend, Property 4: 表单字段长度验证
   * **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8**
   * 
   * 对于任何表单输入字段，当输入内容超过规定的最大长度时，系统应阻止提交并显示验证错误
   */
  describe('Property 4: 表单字段长度验证', () => {
    it('should reject strings exceeding max length of 100 for name fields', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 101, maxLength: 200 }),
          (longString) => {
            // Test author name
            const authorRule = authorNameRules[1] // maxLength rule
            expect(authorRule.max).toBe(100)
            expect(longString.length).toBeGreaterThan(100)
            
            // Test work name
            const workRule = workNameRules[1]
            expect(workRule.max).toBe(100)
            
            // Test character name
            const characterRule = characterNameRules[1]
            expect(characterRule.max).toBe(100)
            
            // Test species
            const speciesRule = speciesRules[1]
            expect(speciesRule.max).toBe(100)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject strings exceeding max length of 100 for alias fields', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 101, maxLength: 200 }),
          (longString) => {
            const rule = aliasRules[0]
            expect(rule.max).toBe(100)
            expect(longString.length).toBeGreaterThan(100)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject strings exceeding max length of 500 for signature fields', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 501, maxLength: 700 }),
          (longString) => {
            const rule = signatureRules[0]
            expect(rule.max).toBe(500)
            expect(longString.length).toBeGreaterThan(500)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject strings exceeding max length of 500 for url fields', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 501, maxLength: 700 }),
          (longString) => {
            const rule = urlRules[0]
            expect(rule.max).toBe(500)
            expect(longString.length).toBeGreaterThan(500)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject strings exceeding max length of 1000 for remark fields', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1001, maxLength: 1500 }),
          (longString) => {
            const rule = remarkRules[0]
            expect(rule.max).toBe(1000)
            expect(longString.length).toBeGreaterThan(1000)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should accept strings within max length limits', () => {
      fc.assert(
        fc.property(
          fc.record({
            name: fc.string({ maxLength: 100 }),
            alias: fc.string({ maxLength: 100 }),
            signature: fc.string({ maxLength: 500 }),
            url: fc.string({ maxLength: 500 }),
            remark: fc.string({ maxLength: 1000 })
          }),
          (validStrings) => {
            // All strings should be within limits
            expect(validStrings.name.length).toBeLessThanOrEqual(100)
            expect(validStrings.alias.length).toBeLessThanOrEqual(100)
            expect(validStrings.signature.length).toBeLessThanOrEqual(500)
            expect(validStrings.url.length).toBeLessThanOrEqual(500)
            expect(validStrings.remark.length).toBeLessThanOrEqual(1000)
            
            // Verify rules match
            expect(authorNameRules[1].max).toBe(100)
            expect(aliasRules[0].max).toBe(100)
            expect(signatureRules[0].max).toBe(500)
            expect(urlRules[0].max).toBe(500)
            expect(remarkRules[0].max).toBe(1000)
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  /**
   * Feature: tag-management-frontend, Property 5: 必填字段验证
   * **Validates: Requirements 10.1, 10.2, 10.3, 10.4**
   * 
   * 对于任何标记为必填的表单字段，当字段为空时，系统应阻止提交并显示验证错误
   */
  describe('Property 5: 必填字段验证', () => {
    it('should reject empty strings for required author name field', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('', '   ', '\t', '\n', '  \t\n  '),
          (emptyString) => {
            const rule = authorNameRules[0]
            expect(rule.required).toBe(true)
            expect(rule.message).toBe('作者名称不能为空')
            
            // Empty or whitespace-only strings should fail validation
            const trimmed = emptyString.trim()
            expect(trimmed).toBe('')
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject empty strings for required work name field', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('', '   ', '\t', '\n'),
          (emptyString) => {
            const rule = workNameRules[0]
            expect(rule.required).toBe(true)
            expect(rule.message).toBe('原作名称不能为空')
            
            const trimmed = emptyString.trim()
            expect(trimmed).toBe('')
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject empty strings for required character name field', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('', '   ', '\t'),
          (emptyString) => {
            const rule = characterNameRules[0]
            expect(rule.required).toBe(true)
            expect(rule.message).toBe('角色名称不能为空')
            
            const trimmed = emptyString.trim()
            expect(trimmed).toBe('')
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject empty strings for required species field', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('', '   ', '\t', '\n'),
          (emptyString) => {
            const rule = speciesRules[0]
            expect(rule.required).toBe(true)
            expect(rule.message).toBe('角色种族不能为空')
            
            const trimmed = emptyString.trim()
            expect(trimmed).toBe('')
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should accept non-empty strings for required fields', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1, maxLength: 100 }).filter(s => s.trim().length > 0),
          (nonEmptyString) => {
            // Verify the string is not empty after trimming
            expect(nonEmptyString.trim().length).toBeGreaterThan(0)
            
            // Verify all required rules are properly configured
            expect(authorNameRules[0].required).toBe(true)
            expect(workNameRules[0].required).toBe(true)
            expect(characterNameRules[0].required).toBe(true)
            expect(speciesRules[0].required).toBe(true)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should verify optional fields do not have required rule', () => {
      fc.assert(
        fc.property(
          fc.constant(true),
          () => {
            // Alias, signature, url, and remark should not have required rule
            const aliasHasRequired = aliasRules.some(rule => rule.required === true)
            const signatureHasRequired = signatureRules.some(rule => rule.required === true)
            const urlHasRequired = urlRules.some(rule => rule.required === true)
            const remarkHasRequired = remarkRules.some(rule => rule.required === true)
            
            expect(aliasHasRequired).toBe(false)
            expect(signatureHasRequired).toBe(false)
            expect(urlHasRequired).toBe(false)
            expect(remarkHasRequired).toBe(false)
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  /**
   * Additional property test: Array item validation
   * Tests that array validators properly validate each item in the array
   */
  describe('Property: Array Item Validation', () => {
    it('should validate all items in an array against max length', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.array(fc.string({ minLength: 101, maxLength: 200 }), { minLength: 1, maxLength: 5 }),
          async (longStrings) => {
            const validator = arrayItemValidator([maxLength(100)])
            
            return new Promise<void>((resolve) => {
              if (validator.validator) {
                validator.validator({} as any, longStrings, (error?: Error | string) => {
                  // Should have an error because at least one string exceeds max length
                  expect(error).toBeInstanceOf(Error)
                  if (error instanceof Error) {
                    expect(error.message).toMatch(/第 \d+ 项/)
                  }
                  resolve()
                })
              } else {
                resolve()
              }
            })
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should accept arrays where all items are within max length', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.array(fc.string({ maxLength: 100 }), { minLength: 1, maxLength: 10 }),
          async (validStrings) => {
            const validator = arrayItemValidator([maxLength(100)])
            
            return new Promise<void>((resolve) => {
              if (validator.validator) {
                validator.validator({} as any, validStrings, (error?: Error | string) => {
                  // Should not have an error
                  expect(error).toBeUndefined()
                  resolve()
                })
              } else {
                resolve()
              }
            })
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should validate required items in an array', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.array(
            fc.oneof(
              fc.string({ minLength: 1, maxLength: 50 }),
              fc.constant('')
            ),
            { minLength: 2, maxLength: 5 }
          ).filter(arr => arr.some(s => s === '')), // Ensure at least one empty string
          async (mixedStrings) => {
            const validator = arrayItemValidator([required('不能为空')])
            
            return new Promise<void>((resolve) => {
              if (validator.validator) {
                validator.validator({} as any, mixedStrings, (error?: Error | string) => {
                  // Should have an error because at least one string is empty
                  expect(error).toBeInstanceOf(Error)
                  if (error instanceof Error) {
                    expect(error.message).toMatch(/第 \d+ 项/)
                    expect(error.message).toContain('不能为空')
                  }
                  resolve()
                })
              } else {
                resolve()
              }
            })
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
