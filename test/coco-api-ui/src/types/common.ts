export interface PaginationState {
  current: number
  size: number
  total: number
}

export interface TableColumn {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  formatter?: (row: any) => string
}
