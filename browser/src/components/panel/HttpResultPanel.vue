<template>
  <el-row v-if="value !== undefined && value !== null" style="font-size: 12px;">
    <el-row>
      <el-row :gutter="24">
        <template v-if="value.response.statusCodeValue === 200">
          <el-tag type="success">{{value.request.url}}</el-tag>
        </template>
        <template v-else>
          <el-tag type="danger">{{value.request.url}}</el-tag>
        </template>
        <template v-if="value.response.statusCodeValue === 200">
          <el-tag type="success">{{value.response.statusCodeValue}}/{{value.response.statusCode}}
          </el-tag>
        </template>
        <template v-else>
          <el-tag type="danger">{{value.response.statusCodeValue}}/{{value.response.statusCode}}
          </el-tag>
        </template>
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
            <el-row style="padding: 0 0px;">
              <div style="margin-bottom: 30px;"></div>
              <el-input type="textarea" :autosize='bodySize'
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
              <el-switch v-model="transfer" active-text="转义" style="margin-bottom: 10px;"></el-switch>
              <el-input type="textarea" :autosize='bodySize' v-bind:value="formatResponseBody" readonly
                        size="small"></el-input>
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

  import {format} from '@/utils'

  export default {
    name: 'HttpResultPanel',
    components: {},
    props: {
      value: Object
    },
    data() {
      return {
        //方法
        format: format,
        //其它属性
        bodySize: {
          minRows: 12,
          maxRows: 12
        },
        transfer: true
      }
    },
    watch: {},//watch
    mounted() {
    },
    computed: {
      formatResponseBody() {
        return this.format(this.value.response.body, this.transfer, true);
      }
    },
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
