import request from '@/utils/request'

export default {

  execute(data) {
    return request({
      url: '/ws/executor/execute',
      method: 'post',
      data
    })
  },

  findWsOperations(data) {
    return request({
      url: '/ws/executor/findWsOperations',
      method: 'post',
      data
    })
  },

  getRequestTemplate(data) {
    return request({
      url: '/ws/executor/getRequestTemplate',
      method: 'post',
      data
    })
  }
}

