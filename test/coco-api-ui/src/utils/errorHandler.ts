import { ElMessage, ElMessageBox } from 'element-plus'
import { ApiError } from './request'

/**
 * Parsed uniqueness conflict error details
 */
export interface ConflictDetails {
  entityType: string
  entityId: string
  name: string
}

/**
 * Parsed reference error details
 */
export interface ReferenceDetails {
  referencedByCharacters?: string[]
  referencedByConfigs?: string[]
  characterCount: number
  configCount: number
}

/**
 * Parse uniqueness conflict error message
 * Expected format: "名称已存在：冲突实体类型=AUTHOR, ID=xxx, 名称=xxx"
 * or "别名已存在：冲突实体类型=WORK, ID=xxx, 别名=xxx"
 */
export function parseConflictError(error: ApiError): ConflictDetails | null {
  try {
    const message = error.message
    const match = message.match(/冲突实体类型=(\w+),\s*ID=([^,]+)(?:,\s*(?:名称|别名)=(.+))?/)
    
    if (match) {
      return {
        entityType: match[1],
        entityId: match[2], // Keep original value without trimming
        name: match[3] || ''
      }
    }
    
    return null
  } catch {
    return null
  }
}

/**
 * Parse reference integrity error details
 */
export function parseReferenceError(error: ApiError): ReferenceDetails {
  const data = error.data || {}
  const referencedByCharacters = data.referencedByCharacters || []
  const referencedByConfigs = data.referencedByConfigs || []
  
  return {
    referencedByCharacters,
    referencedByConfigs,
    characterCount: referencedByCharacters.length,
    configCount: referencedByConfigs.length
  }
}

/**
 * Get entity type display name in Chinese
 */
export function getEntityTypeName(entityType: string): string {
  const typeMap: Record<string, string> = {
    'AUTHOR': '作者',
    'WORK': '原作',
    'CHARACTER': '角色',
    'CONFIG': '配置'
  }
  
  // Use hasOwnProperty to avoid prototype pollution
  return Object.prototype.hasOwnProperty.call(typeMap, entityType) ? typeMap[entityType] : entityType
}

/**
 * Handle uniqueness conflict error with detailed message
 */
export function handleConflictError(error: ApiError): void {
  const details = parseConflictError(error)
  
  if (details) {
    const entityTypeName = getEntityTypeName(details.entityType)
    const message = `该名称已被其他${entityTypeName}使用（ID: ${details.entityId}）`
    
    ElMessageBox.alert(message, '唯一性冲突', {
      type: 'warning',
      confirmButtonText: '确定'
    })
  } else {
    // Fallback to original error message
    ElMessage.error(error.message)
  }
}

/**
 * Handle reference integrity error with detailed message and force delete option
 * Returns a promise that resolves to true if user confirms force delete, false otherwise
 */
export async function handleReferenceError(
  error: ApiError,
  entityName: string,
  onForceDelete: () => Promise<void>
): Promise<boolean> {
  const details = parseReferenceError(error)
  
  let message = `无法删除 "${entityName}"，该项被以下内容引用：\n\n`
  
  if (details.characterCount > 0) {
    message += `• 角色: ${details.characterCount} 个\n`
  }
  
  if (details.configCount > 0) {
    message += `• 标签过滤配置: ${details.configCount} 个\n`
  }
  
  message += '\n是否强制删除？（将自动清理所有引用关系）'
  
  try {
    await ElMessageBox.confirm(message, '引用关系检查', {
      confirmButtonText: '强制删除',
      cancelButtonText: '取消',
      type: 'warning',
      dangerouslyUseHTMLString: false
    })
    
    // User confirmed, execute force delete
    try {
      await onForceDelete()
      ElMessage.success('强制删除成功')
      return true
    } catch (forceError) {
      ElMessage.error('强制删除失败')
      console.error('Force delete failed:', forceError)
      return false
    }
  } catch {
    // User cancelled
    return false
  }
}

/**
 * Show success message for operations
 */
export function showSuccessMessage(message: string): void {
  ElMessage.success(message)
}

/**
 * Show error message for operations
 */
export function showErrorMessage(message: string): void {
  ElMessage.error(message)
}

/**
 * Show confirmation dialog before dangerous operations
 */
export async function confirmDangerousOperation(
  message: string,
  title: string = '确认操作'
): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}
