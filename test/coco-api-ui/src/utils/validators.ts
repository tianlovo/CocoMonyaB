import type { FormItemRule } from 'element-plus'

/**
 * 验证规则函数集合
 * 用于 Element Plus Form 组件的表单验证
 */

/**
 * 必填字段验证规则
 * @param message 错误提示消息
 * @returns Element Plus 验证规则
 */
export const required = (message: string = '此字段为必填项'): FormItemRule => ({
  required: true,
  message,
  trigger: ['blur', 'change']
})

/**
 * 字符串长度验证规则
 * @param max 最大长度
 * @param message 错误提示消息（可选）
 * @returns Element Plus 验证规则
 */
export const maxLength = (max: number, message?: string): FormItemRule => ({
  max,
  message: message || `长度不能超过 ${max} 个字符`,
  trigger: ['blur', 'change']
})

/**
 * 字符串长度范围验证规则
 * @param min 最小长度
 * @param max 最大长度
 * @param message 错误提示消息（可选）
 * @returns Element Plus 验证规则
 */
export const lengthRange = (min: number, max: number, message?: string): FormItemRule => ({
  min,
  max,
  message: message || `长度必须在 ${min} 到 ${max} 个字符之间`,
  trigger: ['blur', 'change']
})

/**
 * 作者名称验证规则
 * 验证：不为空且长度不超过 100
 */
export const authorNameRules: FormItemRule[] = [
  required('作者名称不能为空'),
  maxLength(100, '作者名称长度不能超过 100 个字符')
]

/**
 * 原作名称验证规则
 * 验证：不为空且长度不超过 100
 */
export const workNameRules: FormItemRule[] = [
  required('原作名称不能为空'),
  maxLength(100, '原作名称长度不能超过 100 个字符')
]

/**
 * 角色名称验证规则
 * 验证：不为空且长度不超过 100
 */
export const characterNameRules: FormItemRule[] = [
  required('角色名称不能为空'),
  maxLength(100, '角色名称长度不能超过 100 个字符')
]

/**
 * 角色种族验证规则
 * 验证：不为空且长度不超过 100
 */
export const speciesRules: FormItemRule[] = [
  required('角色种族不能为空'),
  maxLength(100, '角色种族长度不能超过 100 个字符')
]

/**
 * 别名验证规则
 * 验证：长度不超过 100
 */
export const aliasRules: FormItemRule[] = [
  maxLength(100, '别名长度不能超过 100 个字符')
]

/**
 * 个性签名验证规则
 * 验证：长度不超过 500
 */
export const signatureRules: FormItemRule[] = [
  maxLength(500, '个性签名长度不能超过 500 个字符')
]

/**
 * 网址验证规则
 * 验证：长度不超过 500
 */
export const urlRules: FormItemRule[] = [
  maxLength(500, '网址长度不能超过 500 个字符')
]

/**
 * 备注验证规则
 * 验证：长度不超过 1000
 */
export const remarkRules: FormItemRule[] = [
  maxLength(1000, '备注长度不能超过 1000 个字符')
]

/**
 * 数组项验证器
 * 用于验证数组中的每一项
 * @param itemRules 单个项的验证规则
 * @returns Element Plus 验证规则
 */
export const arrayItemValidator = (itemRules: FormItemRule[]): FormItemRule => ({
  validator: (_rule, value, callback) => {
    if (!Array.isArray(value)) {
      callback()
      return
    }

    // 验证数组中的每一项
    for (let i = 0; i < value.length; i++) {
      const item = value[i]
      for (const itemRule of itemRules) {
        // 检查长度限制
        if (itemRule.max !== undefined && typeof item === 'string' && item.length > itemRule.max) {
          callback(new Error(`第 ${i + 1} 项：${itemRule.message || '长度超出限制'}`))
          return
        }
        // 检查必填
        if (itemRule.required && (!item || (typeof item === 'string' && item.trim() === ''))) {
          callback(new Error(`第 ${i + 1} 项：${itemRule.message || '不能为空'}`))
          return
        }
      }
    }
    callback()
  },
  trigger: ['blur', 'change']
})

/**
 * 别名列表验证规则
 * 验证数组中每个别名的长度不超过 100
 */
export const aliasListRules: FormItemRule[] = [
  arrayItemValidator(aliasRules)
]

/**
 * 网址列表验证规则
 * 验证数组中每个网址的长度不超过 500
 */
export const urlListRules: FormItemRule[] = [
  arrayItemValidator(urlRules)
]
