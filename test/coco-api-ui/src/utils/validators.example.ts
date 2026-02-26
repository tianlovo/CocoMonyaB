/**
 * 验证规则使用示例
 * 
 * 本文件展示如何在 Element Plus Form 组件中使用验证规则
 */

import type { FormRules } from 'element-plus'
import {
  authorNameRules,
  workNameRules,
  characterNameRules,
  speciesRules,
  aliasListRules,
  signatureRules,
  urlListRules,
  remarkRules,
  required,
  maxLength
} from './validators'

/**
 * 作者表单验证规则示例
 */
export const authorFormRules: FormRules = {
  name: authorNameRules,
  aliases: aliasListRules,
  signature: signatureRules,
  urls: urlListRules,
  remark: remarkRules
}

/**
 * 原作表单验证规则示例
 */
export const workFormRules: FormRules = {
  name: workNameRules,
  aliases: aliasListRules,
  urls: urlListRules,
  remark: remarkRules
}

/**
 * 角色表单验证规则示例
 */
export const characterFormRules: FormRules = {
  name: characterNameRules,
  species: speciesRules,
  aliases: aliasListRules,
  remark: remarkRules
}

/**
 * 自定义验证规则示例
 * 
 * 如果需要自定义验证规则，可以使用 required 和 maxLength 函数
 */
export const customFormRules: FormRules = {
  // 必填字段，自定义错误消息
  customField1: [
    required('请输入自定义字段1'),
    maxLength(200, '自定义字段1长度不能超过200个字符')
  ],
  
  // 可选字段，仅限制长度
  customField2: [
    maxLength(300)
  ]
}

/**
 * 在 Vue 组件中使用示例：
 * 
 * <template>
 *   <el-form
 *     ref="formRef"
 *     :model="formData"
 *     :rules="authorFormRules"
 *     label-position="top"
 *   >
 *     <el-form-item label="作者名称" prop="name">
 *       <el-input v-model="formData.name" />
 *     </el-form-item>
 *     
 *     <el-form-item label="个性签名" prop="signature">
 *       <el-input v-model="formData.signature" type="textarea" />
 *     </el-form-item>
 *     
 *     <el-form-item label="备注" prop="remark">
 *       <el-input v-model="formData.remark" type="textarea" />
 *     </el-form-item>
 *   </el-form>
 * </template>
 * 
 * <script setup lang="ts">
 * import { ref } from 'vue'
 * import type { FormInstance } from 'element-plus'
 * import { authorFormRules } from '@/utils/validators.example'
 * 
 * const formRef = ref<FormInstance>()
 * const formData = ref({
 *   name: '',
 *   signature: '',
 *   remark: ''
 * })
 * 
 * const submitForm = async () => {
 *   if (!formRef.value) return
 *   
 *   try {
 *     await formRef.value.validate()
 *     // 验证通过，提交表单
 *     console.log('表单数据:', formData.value)
 *   } catch (error) {
 *     // 验证失败
 *     console.error('表单验证失败:', error)
 *   }
 * }
 * </script>
 */
