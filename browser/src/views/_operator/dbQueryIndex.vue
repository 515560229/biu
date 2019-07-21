<template>
  <div class="app-container">
    <!--查询  -->
    <el-row>
      <el-select
        v-perm="'b:operator:dbQuery:query'" size="small" v-model="dbQueryNameId" filterable remote clearable placeholder="请输入查询名称"
        :remote-method="findDbQueryNames"
        :loading="dbQueryNamesLoading">
        <el-option
          v-for="item in dbQueryNamesData"
          :key="item.id"
          :label="item.name"
          :value="item.id">
        </el-option>
      </el-select>
      <span style="margin-right: 45px;"></span>
      <el-tooltip content="执行" placement="top">
        <el-button type="primary" icon="el-icon-caret-right" circle plain @click="executeData"
                   v-perm="'b:operator:common:execute'">
        </el-button>
      </el-tooltip>
      <el-tooltip content="新增" placement="top">
        <el-button type="primary" icon="el-icon-plus" circle plain @click="handleCreate"
                   v-perm="'b:operator:dbQuery:add'">
        </el-button>
      </el-tooltip>
      <el-tooltip content="删除" placement="top">
        <el-button type="danger" icon="el-icon-delete" circle plain @click="deleteData"
                   v-perm="'b:operator:dbQuery:delete'">
        </el-button>
      </el-tooltip>
    </el-row>
    <!--列表-->
    <el-table style="width: 100%"
              :data="listResults"
              v-if="listShow"
              v-loading.body="listLoading"
              element-loading-text="加载中"
              border fit highlight-current-row
              @cell-dblclick="handleCellDbClick"
    >
      <el-table-column :prop="column" :label="column" v-for="(column,index) in listColumns" :show-overflow-tooltip='true' :key="index">
        <template slot-scope="scope">
          <span v-text="scope.row[column]"></span>
        </template>
      </el-table-column>
    </el-table>
    <!--弹出窗口：新增/编辑-->
    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="80%">
      <el-form :rules="rules" ref="dataForm" :model="temp" label-position="left" label-width="120px">

        <el-form-item label="名称" prop="name" v-if="dialogStatus=='create'">
          <el-input v-model="temp.name"></el-input>
        </el-form-item>

        <el-form-item label="数据源" prop="dbConfig">
          <el-select v-model="temp.dbQueryConfig.id" filterable remote reserve-keyword clearable style="width:100%"
                     placeholder="请输入数据源名称,支持模糊搜索"
                     :remote-method="findDbConfig"
                     :loading="dbNamesLoading">
            <el-option
              v-for="item in dbNames"
              :key="item.id"
              :label="item.name"
              :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="SQL模板" prop="sqlTemplate">
          <el-input type="textarea" v-model="temp.dbQueryConfig.sqlTemplate" rows="10"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button v-if="dialogStatus=='create'" type="primary" @click="createData">创建</el-button>
        <el-button v-else type="primary" @click="updateData">确定</el-button>
      </div>
    </el-dialog>
    <el-dialog :visible.sync="formatDialogVisible" width="60%">
      <json-editor :value="needFormatValue"></json-editor>
    </el-dialog>
  </div>
</template>

<script>

  import commonConfigApi from '@/api/config/commonConfig'
  import dbOperateApi from '@/api/operate/dbOperateApi'
  import {parseTime, resetTemp, isJsonString} from '@/utils'
  import {confirm, pageParamNames, root} from '@/utils/constants'
  import debounce from 'lodash/debounce'
  import {jsonEditor} from '@/components/JsonEditor'
  import JsonEditor from "../../components/JsonEditor/index";

  export default {

    name: 'DbQueryManage',
    components: {JsonEditor},
    data() {

      let validateNotNull = (rule, value, callback) => {
        if (this.dialogStatus == 'create' && value === '') {
          callback(new Error('必填'));
        } else {
          callback();
        }
      };

      return {
        type: "dbQuery",
        storageKey: "dbQuery",
        parseTime: parseTime,
        isJsonString: isJsonString,
        dbQueryNameId: null,
        dbQueryNamesLoading: false,
        dbQueryNames: [],
        dbQueryNamesData: [],
        dbQueryNamesQuery: {
          key: null,
          type: "dbQuery"
        },
        dbQueryNamesPage: {
          current: null,
          pages: null,
          size: null,
          total: null
        },
        //数据源查询相关变量
        dbNames: [],
        dbNamesLoading: false,
        dbNamesPage: {
          current: null,
          pages: null,
          size: null,
          total: null
        },
        dbNamesQuery: {
          key: null,
          type: "db"
        },
        //查询结果相关
        listResults: [],//表数据
        listColumns: [],//表头
        listLoading: false,//是否显示加载框
        listShow: false,//是否显示表格
        //格式化相关
        needFormatValue: null,
        formatDialogVisible: false,

        dialogFormVisible: false,
        editRolesDialogVisible: false,
        dialogStatus: '',
        temp: {
          idx: null, //tableData中的下标
          id: null,
          type: null,
          name: null,
          desc: null,
          created: null,
          updated: null,
          dbQueryConfig: {
            id: null,
            sqlTemplate: null
          }
        },
        textMap: {
          update: '编辑',
          create: '新增'
        },
        rules: {},
        checkAll: false,
        isIndeterminate: true,
      }
    },

    created() {
      // 在create后还原数据, 实现页面数据状态保存
      let tempData = JSON.parse(localStorage.getItem(this.storageKey));
      for (let key in this._data) {
        if (tempData[key]) {
          //原来有值才使用
          this.$set(this._data, key, tempData[key]);
        }
      }
    },
    mounted() {
    },
    beforeDestroy() {

    },
    destroyed() {
      // 在destroy后保存数据
      localStorage.setItem(this.storageKey, JSON.stringify(this._data));
    },

    watch: {},//watch

    methods: {//新增
      //数据库查询语句的相关操作
      findDbQueryNames(key) {
        if (key !== '') {
          this.dbQueryNamesLoading = true;

          this.dbQueryNamesQuery.key = key;
          commonConfigApi.queryCommonConfig(this.dbQueryNamesQuery, this.dbQueryNamesPage).then(res => {
            this.dbQueryNamesData = res.data.page.records
            this.dbQueryNamesLoading = false
          })
        } else {
          this.dbQueryNamesData = [];
        }
      },
      handleCellDbClick(row, column, cell, event) {
        let str = row[column.property];
        if (isJsonString(str)) {
          this.formatDialogVisible = true;
          this.needFormatValue = JSON.parse(row[column.property]);
        }
      },
      handleCreate() {
        resetTemp(this.temp)
        this.dialogStatus = 'create'
        this.dialogFormVisible = true
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        })
      },
      createData() {
        this.$refs['dataForm'].validate((valid) => {
          if (!valid) {
            return;
          }
          let tempData = Object.assign({}, this.temp)//copy obj
          tempData.type = this.type;
          commonConfigApi.addCommonConfig(tempData).then((res) => {
            this.temp = res.data.data;
            this.dialogFormVisible = false
            this.$message.success("添加成功")
          })
        })
      },
      deleteData() {
        this.$confirm('您确定要永久删除该查询语句？', '提示', confirm).then(() => {
          commonConfigApi.deleteCommonConfig(this.dbQueryNameId).then(res => {
            this.$message.success("删除成功");
            this.dbQueryNamesData = [];
            this.dbQueryNameId = null;
          })
        }).catch(() => {
          this.$message.info("已取消删除")
        });
      },
      executeData() {
        if (this.dbQueryNamesData) {
          for (let i = 0; i < this.dbQueryNamesData.length; i++) {
            if (this.dbQueryNamesData[i].id == this.dbQueryNameId) {
              this.listLoading = true;
              dbOperateApi.execute(this.dbQueryNamesData[i]).then(res => {
                this.listShow = true;
                this.listLoading = false;
                this.listResults = res.data.data;
                if (this.listResults && this.listResults.length > 0) {
                  this.listColumns = Object.keys(this.listResults[0]);
                  console.log(this.listColumns);
                }
              });
            }
          }
        }
      },
      updateData() {

      },
      //查找数据源
      findDbConfig(key) {
        if (key !== '') {
          this.dbNamesLoading = true;

          this.dbNamesQuery.key = key;
          commonConfigApi.queryCommonConfig(this.dbNamesQuery, this.dbNamesPage).then(res => {
            this.dbNames = res.data.page.records
            this.dbNamesLoading = false
          })
        } else {
          this.dbNames = [];
        }
      }
    }
  }
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
  .role-checkbox {
    margin-left: 0px;
    margin-right: 15px;
  }
</style>
