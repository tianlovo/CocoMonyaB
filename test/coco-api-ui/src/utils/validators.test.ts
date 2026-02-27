import { describe, it, expect } from 'vitest'
import {
  required,
  maxLength,
  lengthRange,
  authorNameRules,
  workNameRules,
  characterNameRules,
  speciesRules,
  aliasRules,
  signatureRules,
  urlRules,
  remarkRules,
  arrayItemValidator,
  aliasListRules,
  urlListRules
} from './validators'

describe('Validators', () => {
  describe('required', () => {
    it('should create a required rule with default message', () => {
      const rule = required()
      expect(rule.required).toBe(true)
      expect(rule.message).toBe('此字段为必填项')
      expect(rule.trigger).toEqual(['blur', 'change'])
    })

    it('should create a required rule with custom message', () => {
      const rule = required('自定义错误消息')
      expect(rule.required).toBe(true)
      expect(rule.message).toBe('自定义错误消息')
    })
  })

  describe('maxLength', () => {
    it('should create a max length rule with default message', () => {
      const rule = maxLength(100)
      expect(rule.max).toBe(100)
      expect(rule.message).toBe('长度不能超过 100 个字符')
      expect(rule.trigger).toEqual(['blur', 'change'])
    })

    it('should create a max length rule with custom message', () => {
      const rule = maxLength(50, '自定义长度错误')
      expect(rule.max).toBe(50)
      expect(rule.message).toBe('自定义长度错误')
    })
  })

  describe('lengthRange', () => {
    it('should create a length range rule with default message', () => {
      const rule = lengthRange(5, 100)
      expect(rule.min).toBe(5)
      expect(rule.max).toBe(100)
      expect(rule.message).toBe('长度必须在 5 到 100 个字符之间')
    })

    it('should create a length range rule with custom message', () => {
      const rule = lengthRange(10, 50, '自定义范围错误')
      expect(rule.min).toBe(10)
      expect(rule.max).toBe(50)
      expect(rule.message).toBe('自定义范围错误')
    })
  })

  describe('authorNameRules', () => {
    it('should have required and max length rules', () => {
      expect(authorNameRules).toHaveLength(2)
      expect(authorNameRules[0].required).toBe(true)
      expect(authorNameRules[0].message).toBe('作者名称不能为空')
      expect(authorNameRules[1].max).toBe(100)
    })
  })

  describe('workNameRules', () => {
    it('should have required and max length rules', () => {
      expect(workNameRules).toHaveLength(2)
      expect(workNameRules[0].required).toBe(true)
      expect(workNameRules[0].message).toBe('原作名称不能为空')
      expect(workNameRules[1].max).toBe(100)
    })
  })

  describe('characterNameRules', () => {
    it('should have required and max length rules', () => {
      expect(characterNameRules).toHaveLength(2)
      expect(characterNameRules[0].required).toBe(true)
      expect(characterNameRules[0].message).toBe('角色名称不能为空')
      expect(characterNameRules[1].max).toBe(100)
    })
  })

  describe('speciesRules', () => {
    it('should have required and max length rules', () => {
      expect(speciesRules).toHaveLength(2)
      expect(speciesRules[0].required).toBe(true)
      expect(speciesRules[0].message).toBe('角色种族不能为空')
      expect(speciesRules[1].max).toBe(100)
    })
  })

  describe('aliasRules', () => {
    it('should have max length rule of 100', () => {
      expect(aliasRules).toHaveLength(1)
      expect(aliasRules[0].max).toBe(100)
    })
  })

  describe('signatureRules', () => {
    it('should have max length rule of 500', () => {
      expect(signatureRules).toHaveLength(1)
      expect(signatureRules[0].max).toBe(500)
    })
  })

  describe('urlRules', () => {
    it('should have max length rule of 500', () => {
      expect(urlRules).toHaveLength(1)
      expect(urlRules[0].max).toBe(500)
    })
  })

  describe('remarkRules', () => {
    it('should have max length rule of 1000', () => {
      expect(remarkRules).toHaveLength(1)
      expect(remarkRules[0].max).toBe(1000)
    })
  })

  describe('arrayItemValidator', () => {
    it('should validate array items with max length', () => {
      return new Promise<void>((resolve) => {
        const validator = arrayItemValidator([maxLength(10)])
        
        // Valid array
        if (validator.validator) {
          validator.validator({} as any, ['short', 'ok'], (error?: Error | string) => {
            expect(error).toBeUndefined()
            resolve()
          })
        }
      })
    })

    it('should reject array items exceeding max length', () => {
      return new Promise<void>((resolve) => {
        const validator = arrayItemValidator([maxLength(10)])
        
        // Invalid array - second item too long
        if (validator.validator) {
          validator.validator({} as any, ['short', 'this is too long'], (error?: Error | string) => {
            expect(error).toBeInstanceOf(Error)
            if (error instanceof Error) {
              expect(error.message).toContain('第 2 项')
            }
            resolve()
          })
        }
      })
    })

    it('should validate array items with required rule', () => {
      return new Promise<void>((resolve) => {
        const validator = arrayItemValidator([required('不能为空')])
        
        // Invalid array - empty item
        if (validator.validator) {
          validator.validator({} as any, ['valid', ''], (error?: Error | string) => {
            expect(error).toBeInstanceOf(Error)
            if (error instanceof Error) {
              expect(error.message).toContain('第 2 项')
              expect(error.message).toContain('不能为空')
            }
            resolve()
          })
        }
      })
    })

    it('should pass validation for non-array values', () => {
      return new Promise<void>((resolve) => {
        const validator = arrayItemValidator([maxLength(10)])
        
        if (validator.validator) {
          validator.validator({} as any, null, (error?: Error | string) => {
            expect(error).toBeUndefined()
            resolve()
          })
        }
      })
    })
  })

  describe('aliasListRules', () => {
    it('should validate alias list items', () => {
      expect(aliasListRules).toHaveLength(1)
      expect(aliasListRules[0].validator).toBeDefined()
    })
  })

  describe('urlListRules', () => {
    it('should validate url list items', () => {
      expect(urlListRules).toHaveLength(1)
      expect(urlListRules[0].validator).toBeDefined()
    })
  })
})
