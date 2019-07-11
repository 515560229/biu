/**
 * “全局变量”相关接口
 */
import request from '@/utils/request'

export default {

  queryVariable(queryParam,pageParam) {
    return request({
      url: '/config/variable/query',
      method: 'post',
      data: {
        ...queryParam,
        current: pageParam.current,
        size: pageParam.size
      }
    })
  },

  updateVariable(data) {
    return request({
      url: '/config/variable',
      method: 'put',
      data
    })
  },

  addVariable(data) {
    return request({
      url: '/config/variable',
      method: 'post',
      data
    })
  },

  deleteUser(data) {
    return request({
      url: '/sys_user',
      method: 'delete',
      data
    })
  }
}

