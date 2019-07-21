import request from '@/utils/request'

export default {

  execute(data) {
    return request({
      url: '/operator/db/execute',
      method: 'post',
      data
    })
  }
}

