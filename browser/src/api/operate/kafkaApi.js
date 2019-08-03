import request from '@/utils/request'

export default {

  consumer(data) {
    return request({
      url: '/kafka/executor/consumer',
      method: 'post',
      data
    })
  },
  producer(data) {
    return request({
      url: '/kafka/executor/producer',
      method: 'post',
      data
    })
  },
}

