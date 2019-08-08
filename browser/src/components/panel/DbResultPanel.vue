<template>
  <el-row>
    <el-row v-if="value !== undefined && value != null" style="font-size: 12px;">
      <div>{{value.executeSQL}}</div>
      <!-- 结果面板 -->
      <el-table style="width: 100%;height: 95%;"
                :data="value.data"
                v-loading.body="value.loading"
                element-loading-text="加载中"
                border fit highlight-current-row
                @cell-dblclick="handleCellDbClick"
      >
        <el-table-column :prop="column" :label="column"
                         v-for="(column,idx) in value.columns"
                         :show-overflow-tooltip='true' :key="idx">
          <template slot-scope="scope">
            <span v-text="scope.row[column]"></span>
          </template>
        </el-table-column>
      </el-table>
    </el-row>
    <el-dialog :visible.sync="formatDialogVisible" width="60%">
      <el-input type="textarea" :autosize='cellDetailsSize'
                v-model="needFormatValue" readonly size="small"></el-input>
    </el-dialog>
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
        //方法
        isJsonString: isJsonString,
        //格式化相关
        needFormatValue: null,
        formatDialogVisible: false,

        cellDetailsSize: {
          minRows: 16,
          maxRows: 20
        }
      }
    },
    watch: {},//watch
    mounted() {
    },
    computed: {},
    methods: {
      handleCellDbClick(row, column, cell, event) {
        let str = row[column.property];
        if (str === undefined || str == null || str === '') {
          return;
        }

        this.formatDialogVisible = true;
        if (isJsonString(str)) {
          this.needFormatValue = pd.json(str);
        } else {
          try {
            this.needFormatValue = pd.xml(str);
          } catch (e) {
            this.needFormatValue = str;
          }
        }
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
