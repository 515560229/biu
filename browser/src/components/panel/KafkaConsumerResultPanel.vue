<template>
  <el-row>
    <el-row v-if="value !== undefined && value != null" style="font-size: 12px;">
      <el-row>
        共扫描{{value.topic}} {{value.totalCount}} 条消息
      </el-row>
      <!-- 结果面板 -->
      <el-table style="width: 100%;height: 95%;"
                :data="value.messages"
                v-loading.body="value.loading"
                element-loading-text="加载中"
                border fit highlight-current-row
                @cell-dblclick="handleCellDbClick"
      >
        <el-table-column prop="partition" label="partition" width="100">
        </el-table-column>
        <el-table-column prop="offset" label="offset" width="100">
        </el-table-column>
        <el-table-column prop="message" label="message"
                         :show-overflow-tooltip='true'>
        </el-table-column>
      </el-table>
    </el-row>
    <el-dialog :visible.sync="formatDialogVisible" width="60%">
      <el-switch v-model="transfer" active-text="转义" style="margin-bottom: 10px;" @change="handleChange"></el-switch>
      <el-input type="textarea" :autosize='cellDetailsSize'
                v-model="needFormatValue" readonly size="small"></el-input>
    </el-dialog>
  </el-row>
</template>
<script>

  import {deepClone, isJsonString, resetTemp, format} from '@/utils'

  export default {
    name: 'KafkaConsumerResultPanel',
    components: {},
    props: {
      value: Object
    },
    data() {
      return {
        //方法
        isJsonString: isJsonString,
        format: format,
        //格式化相关
        needFormatValue: null,
        formatDialogVisible: false,
        transfer: true,
        oldValue: null,

        cellDetailsSize: {
          minRows: 16,
          maxRows: 20
        }
      }
    },
    watch: {
    },//watch
    mounted() {
    },
    computed: {},
    methods: {
      showFormat() {
        if (this.oldValue === undefined || this.oldValue == null || this.oldValue === '') {
          return;
        }
        this.formatDialogVisible = true;
        this.needFormatValue = format(this.oldValue, this.transfer, true);
      },
      handleCellDbClick(row, column, cell, event) {
        let str = row[column.property];
        this.oldValue = str;
        this.showFormat();
      },
      handleChange(val) {
        this.showFormat();
      }
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
