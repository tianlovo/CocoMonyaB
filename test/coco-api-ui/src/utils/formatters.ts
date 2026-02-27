/**
 * Utility functions for data formatting in message tracking visualization
 */

/**
 * Format timestamp to YYYY-MM-DD HH:mm:ss format
 * @param dateTimeStr - ISO timestamp string or Date object
 * @returns Formatted date string or '-' for invalid/empty values
 */
export const formatDateTime = (dateTimeStr: string | Date | null | undefined): string => {
  if (!dateTimeStr) return '-'
  
  try {
    const date = typeof dateTimeStr === 'string' ? new Date(dateTimeStr) : dateTimeStr
    
    // Check if date is valid
    if (isNaN(date.getTime())) return '-'
    
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  } catch {
    return '-'
  }
}

/**
 * Format boolean value to Chinese display
 * @param value - Boolean value
 * @returns '是' for true, '否' for false
 */
export const formatBoolean = (value: boolean | null | undefined): string => {
  if (value === null || value === undefined) return '-'
  return value ? '是' : '否'
}

/**
 * Get Element Plus tag type for status value
 * @param status - Status string
 * @returns Tag type for Element Plus el-tag component
 */
export const getStatusType = (status: string | null | undefined): 'success' | 'warning' | 'danger' | 'info' => {
  if (!status) return 'info'
  
  const statusMap: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    completed: 'success',
    processed: 'success',
    success: 'success',
    approved: 'success',
    processing: 'warning',
    rejected: 'danger',
    failed: 'danger',
    pending: 'info'
  }
  
  return statusMap[status.toLowerCase()] || 'info'
}

/**
 * Get Chinese label for status value
 * @param status - Status string
 * @returns Chinese label for the status
 */
export const getStatusLabel = (status: string | null | undefined): string => {
  if (!status) return '-'
  
  const labelMap: Record<string, string> = {
    pending: '待处理',
    processing: '处理中',
    processed: '已处理',
    completed: '已完成',
    success: '转发成功',
    approved: '已通过',
    rejected: '已拒绝',
    failed: '失败'
  }
  
  return labelMap[status.toLowerCase()] || status
}

/**
 * Truncate text exceeding max length and append ellipsis
 * @param text - Text to truncate
 * @param maxLength - Maximum length (default: 100)
 * @returns Truncated text with '...' or '-' for empty values
 */
export const truncateText = (text: string | null | undefined, maxLength: number = 100): string => {
  if (!text) return '-'
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}
