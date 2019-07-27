<template>
  <el-row v-if="value !== undefined" style="font-size: 12px;">
    <el-row>
      <el-row :gutter="24">
        <el-col :span="12">
          <template v-if="value.response.statusCodeValue === 200">
            <el-tag type="success">{{value.request.url}}</el-tag>
          </template>
          <template v-else>
            <el-tag type="danger">{{value.request.url}}</el-tag>
          </template>
        </el-col>
        <el-col :span="12" style="text-align: right;">
          <template v-if="value.response.statusCodeValue === 200">
            <el-tag type="success">{{value.response.statusCodeValue}}/{{value.response.statusCode}}
            </el-tag>
          </template>
          <template v-else>
            <el-tag type="danger">{{value.response.statusCodeValue}}/{{value.response.statusCode}}
            </el-tag>
          </template>
        </el-col>
      </el-row>
      <!-- http headers -->
      <el-row :gutter="24">
        <el-col :span="12">
          headers:
        </el-col>
        <el-col :span="12">
          headers:
        </el-col>
      </el-row>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-row style="padding-left: 20px;">
            <el-row v-for="(val, name) in value.request.headers" :key="name">
              {{name}}:{{val}}
            </el-row>
          </el-row>
        </el-col>
        <el-col :span="12">
          <el-row style="padding-left: 20px;">
            <el-row v-for="(val, name) in value.response.headers" :key="name">
              {{name}}:{{val}}
            </el-row>
          </el-row>
        </el-col>
      </el-row>
      <el-row :gutter="24">
        <el-col :span="12">
          <template v-if="value.request.body != null">
            requestBody:
            <el-row style="padding: 0 20px;">
              <el-input type="textarea" :autosize='requestBodySize'
                        v-model="value.request.body" readonly size="small"></el-input>
            </el-row>
          </template>
          <template v-else>
            &nbsp;
          </template>
        </el-col>
        <el-col :span="12">
          <template v-if="value.response.body != null">
            responseBody:
            <el-row style="padding: 0 20px;">
              <el-input type="textarea" :autosize='requestBodySize'
                        v-model="value.response.body" readonly size="small"></el-input>
            </el-row>
          </template>
          <template v-else>
            &nbsp;
          </template>
        </el-col>
      </el-row>
    </el-row>
  </el-row>
</template>
<script>

  import {deepClone, isJsonString, resetTemp} from '@/utils'
  import debounce from 'lodash/debounce'
  //https://github.com/vkiryukhin/pretty-data
  let pd = require('pretty-data').pd;

  export default {
    name: 'HttpResultPanel',
    components: {},
    props: {
      value: Object
    },
    data() {
      return {
        requestBodySize: {
          minRows: 12,
          maxRows: 12
        }
      }
    },
    watch: {
    },//watch
    mounted() {
    },
    computed: {},
    methods: {}
  }
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
</style>
<style rel="stylesheet/scss" lang="scss">
  .el-select .el-input {
    width: 130px;
  }

  .el-table__expanded-cell[class*=cell] {
    padding: 4px 10px;
  }
</style>
