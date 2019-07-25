import request from '@/utils/request'

export default {

    execute(data) {
    return request({
      url: '/http/executor/execute',
      method: 'post',
      data
    })
  }
}

