/**
 * “通用配置”相关接口
 */
import request from '@/utils/request'

export default {

  queryCommonConfig(queryParam, pageParam) {
    return request({
      url: '/config/common/query',
      method: 'post',
      data: {
        ...queryParam,
        current: pageParam.current,
        size: pageParam.size
      }
    })
  },

  updateCommonConfig(data) {
    return request({
      url: '/config/common',
      method: 'put',
      data
    })
  },

  addCommonConfig(data) {
    return request({
      url: '/config/common',
      method: 'post',
      data
    })
  },

  deleteCommonConfig(id) {
    return request({
      url: '/config/common/id',
      method: 'delete'
    })
  }
}

