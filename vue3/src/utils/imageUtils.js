/**
 * 图片工具函数
 * 提供缩略图路径解析、首张图片提取等功能
 */

const baseAPI = import.meta.env.VITE_BASE_API || '/api'

/**
 * 获取图片的完整URL（自动添加API前缀）
 */
export const getImageUrl = (path) => {
  if (!path) return ''
  return path.startsWith('http') ? path : baseAPI + path
}

/**
 * 获取缩略图路径
 * @param {string} originalPath - 原始图片路径（如 /img/lost/123456.jpg）
 * @returns {string} 缩略图路径
 */
export const getThumbnailPath = (originalPath) => {
  if (!originalPath) return ''
  const lastSlash = originalPath.lastIndexOf('/')
  if (lastSlash < 0) return originalPath
  const dir = originalPath.substring(0, lastSlash)
  const filename = originalPath.substring(lastSlash + 1)
  return `${dir}/thumb_${filename}`
}

/**
 * 获取缩略图完整URL
 */
export const getThumbnailUrl = (path) => {
  return getImageUrl(getThumbnailPath(path))
}

/**
 * 从逗号分隔的图片字符串中提取第一张图片路径
 * @param {string} images - 逗号分隔的图片路径字符串
 * @param {boolean} useThumb - 是否使用缩略图（默认false，列表页建议传true）
 * @returns {string} 首张图片的完整URL
 */
export const getFirstImage = (images, useThumb = false) => {
  if (!images) return ''
  const imageList = images.split(',')
  const firstImage = imageList[0]?.trim() || ''
  if (!firstImage) return ''

  const path = useThumb ? getThumbnailPath(firstImage) : firstImage
  return getImageUrl(path)
}

/**
 * 从逗号分隔的图片字符串中提取所有图片URL列表
 * @param {string} images - 逗号分隔的图片路径字符串
 * @param {boolean} useThumb - 是否使用缩略图
 * @returns {string[]} 图片URL数组
 */
export const getImageList = (images, useThumb = false) => {
  if (!images) return []
  return images.split(',').map(img => {
    const trimmed = img.trim()
    if (!trimmed) return ''
    const path = useThumb ? getThumbnailPath(trimmed) : trimmed
    return getImageUrl(path)
  }).filter(Boolean)
}
