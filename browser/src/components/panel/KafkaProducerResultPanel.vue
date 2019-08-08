<template>
  <el-row>
    <el-row v-if="value !== undefined && value != null" style="font-size: 12px;">
      <template v-if="value.result">
        <el-tag type="success">成功</el-tag>
      </template>
      <template v-else>
        <el-tag type="danger">失败</el-tag>
      </template>
      <!-- 结果面板 -->
      <el-input style="margin-top: 4px" v-model="value.message" type="textarea" :autosize="cellDetailsSize"></el-input>
    </el-row>
  </el-row>
</template>
<script>

  import {deepClone, isJsonString, resetTemp} from '@/utils'
  import debounce from 'lodash/debounce'
  //https://github.com/vkiryukhin/pretty-data
  let pd = require('pretty-data').pd;

  export default {
    name: 'KafkaProducerResultPanel',
    components: {},
    props: {
      value: Object
    },
    data() {
      return {
        //方法
        isJsonString: isJsonString,
        cellDetailsSize: {
          minRows: 16,
          maxRows: 16
        }
      }
    },
    watch: {},//watch
    mounted() {
    },
    computed: {},
    methods: {
    }
  }
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
  .el-table__expanded-cell[class*=cell] {
    padding: 4px 10px;
  }
</style>
<style rel="stylesheet/scss" lang="scss">
</style>
