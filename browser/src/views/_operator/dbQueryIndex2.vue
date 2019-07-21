<template>
  <div class="app-container">
    <el-row :gutter="24" style="height: 400px;">
      <el-table
        :data="dbQueryNamesData"
        style="width: 100%">
        <el-table-column type="expand">
          <template slot-scope="props">
            <el-form label-position="left">
              <el-form-item label="名称">
                <el-input v-model="props.row.name" placeholder="请输入名称" style="width: 200px;"></el-input>
              </el-form-item>
              <el-form-item label="SQL">
                <mysql-editor v-model="props.row.dbQueryConfig.sqlTemplate"></mysql-editor>
              </el-form-item>
            </el-form>
          </template>
        </el-table-column>
        <el-table-column
          type="index"
          width="30">
        </el-table-column>
        <el-table-column
          label="名称"
          prop="name">
        </el-table-column>
        <el-table-column align="right">
          <template slot="header" slot-scope="scope">
            <el-input
              v-model="dbQueryNamesQuery.key"
              size="mini"
              placeholder="输入关键字搜索"/>
          </template>
          <template slot-scope="scope">
            <el-tooltip content="删除" placement="top">
              <el-button @click="deleteData(scope.$index,scope.row)" size="mini" type="danger" icon="el-icon-delete"
                         circle plain></el-button>
            </el-tooltip>
            <el-tooltip content="编辑" placement="top">
              <el-button @click="updateData(scope.$index,scope.row)" size="mini" type="info" icon="el-icon-edit"
                         circle plain></el-button>
            </el-tooltip>
            <el-tooltip content="执行" placement="top">
              <el-button @click="executeData(scope.$index,scope.row)" size="mini" type="info"
                         icon="el-icon-caret-right" circle plain></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-row>
    <el-row :gutter="24" style="margin-top: 10px;">
      <div class="el-tabs el-tabs--top el-tabs--border-card" style="text-align: left;">
        <div class="el-tabs__header is-top">
          <div class="el-tabs__nav-wrap is-top">
            <div class="el-tabs__nav-scroll">
              <div role="tablist" class="el-tabs__nav" style="transform: translateX(0px);">
                <div v-for="tabName in tabsCount" v-bind:id="'tab-' + tabName"
                     v-bind:class="{'el-tabs__item':true,'is-top':true,'is-active': tabIndex === toString(tabName)}"
                     @click="handleTabsClick(toString(tabName))">
                  {{tabName}}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div style="height: 10px;float:left;"></div>
      <!-- 结果面板 -->
      <el-table style="width: 100%;height: 95%;"
                :data="listResults"
                v-loading.body="listLoading"
                v-if="listShow"
                element-loading-text="加载中"
                border fit highlight-current-row
                @cell-dblclick="handleCellDbClick"
      >
        <el-table-column :prop="column" :label="column" v-for="(column,idx) in listColumns"
                         :show-overflow-tooltip='true' :key="idx">
          <template slot-scope="scope">
            <span v-text="scope.row[column]"></span>
          </template>
        </el-table-column>
      </el-table>
    </el-row>
    <el-dialog :visible.sync="formatDialogVisible" width="60%">
      <json-editor v-model="needFormatValue" v-if="textFormat === 'json'"></json-editor>
      <div v-if="textFormat === 'text'">{{needFormatValue}}</div>
    </el-dialog>
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
  </div>
</template>

<script>

  import commonConfigApi from '@/api/config/commonConfig'
  import dbOperateApi from '@/api/operate/dbOperateApi'
  import {parseTime, resetTemp, isJsonString} from '@/utils'
  import {confirm, pageParamNames, root} from '@/utils/constants'
  import debounce from 'lodash/debounce'
  import JsonEditor from "../../components/JsonEditor/index";
  import MysqlEditor from "../../components/MysqlEditor/index";

  export default {
    name: 'DbQueryManage',
    components: {JsonEditor, MysqlEditor},
    data() {

      let validateNotNull = (rule, value, callback) => {
        if (this.dialogStatus == 'create' && value === '') {
          callback(new Error('必填'));
        } else {
          callback();
        }
      };
      let initTableData = function (length) {
        let arr = [];
        for (let i = 0; i < length; i++) {
          arr[i] = '';
        }
        return arr;
      };
      let tabsCount = 10;

      return {
        type: "dbQuery",
        storageKey: "dbQuery2",
        isJsonString: isJsonString,
        //DB查询列表 相关的变量
        dbQueryNameKey: null,
        dbQueryNamesLoading: false,
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
        listColumns: ["dynaColumn"],//表头
        listLoading: false,//是否显示加载框
        listShow: false,//是否显示表格
        //格式化相关
        needFormatValue: null,
        formatDialogVisible: false,
        textFormat: null,
        //tab相关
        tabsCount: tabsCount,
        tabsData: initTableData(tabsCount),
        tabIndex: '1',
        //
        dialogFormVisible: false,
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
      }
    },

    created() {
      // 在create后还原数据, 实现页面数据状态保存
      let tempDataStr = localStorage.getItem(this.storageKey);
      if (tempDataStr) {
        let tempData = JSON.parse(localStorage.getItem(this.storageKey));
        for (let key in this._data) {
          if (tempData[key]) {
            //原来有值才使用
            this.$set(this._data, key, tempData[key]);
          }
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

    watch: {
      //延时查询
      'dbQueryNamesQuery.key': debounce(function () {
        this.findDbQueryNames()
      }, 300)
    },//watch
    computed: {},
    methods: {
      toString(num) {
        return num.toString();
      },
      //SQL相关方法
      onCmReady(cm) {
        console.log('the editor is readied!', cm)
      },
      onCmFocus(cm) {
        console.log('the editor is focus!', cm)
      },
      onCmCodeChange(newCode) {
        console.log('this is new code', newCode)
        this.code = newCode
      },
      handleInput(val) {
        console.log("22222   " + val);
      },

      //新增
      //数据库查询语句的相关操作
      findDbQueryNames() {
        if (this.dbQueryNamesQuery.key !== '') {
          this.dbQueryNamesLoading = true;

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
          this.textFormat = 'json'
          this.formatDialogVisible = true;
          this.needFormatValue = JSON.parse(row[column.property]);
          return;
        }
        this.textFormat = 'text'
        this.formatDialogVisible = true;
        this.needFormatValue = str;
      },
      handleTabsClick(tabName) {
        this.tabIndex = tabName;
        let tabIndex = parseInt(tabName) - 1;
        if (tabIndex >= this.tabsData.length) {
          //打开新的tab页
          // this.listColumns = null;
          this.listShow = false;
          // this.listResults = null;
          // this.listLoading = false;
        } else {
          this.listColumns = this.tabsData[tabIndex].listColumns;
          this.listShow = this.tabsData[tabIndex].listShow;
          this.listResults = this.tabsData[tabIndex].listResults;
          this.listLoading = this.tabsData[tabIndex].listLoading;
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
      updateData(idx, dbQueryName) {
        commonConfigApi.updateCommonConfig(dbQueryName).then(res => {
          this.$message.success("保存成功")
        });
      },
      deleteData(idx, dbQueryName) {
        this.$confirm('您确定要永久删除该查询语句？', '提示', confirm).then(() => {
          commonConfigApi.deleteCommonConfig(dbQueryName.id).then(res => {
            this.$message.success("删除成功");
            this.dbQueryNamesData.splice(idx, 1);
          })
        }).catch(() => {
          this.$message.info("已取消删除")
        });
      },
      executeData(idx, dbQueryName) {
        this.tabIndex = this.toString(idx + 1);
        this.listLoading = true;
        dbOperateApi.execute(dbQueryName).then(res => {
          let data = res.data.data;
          let listColumns = [];
          if (data && data.length > 0) {
            listColumns = Object.keys(data[0]);
          }
          this.listResults = data;
          this.listShow = true;
          this.listLoading = false;
          this.listColumns = listColumns;
          this.tabsData.splice(parseInt(this.tabIndex) - 1, 1, {
            listResults: data,
            listShow: true,
            listLoading: false,
            listColumns: listColumns
          });
        }).catch(e => {
          this.listLoading = false;
        });
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

  [v-cloak] {
    display: none;
  }

  .el-tabs .el-tabs__item.is-active {
    color: black;
    background-color: green;
    border-right-color: #DCDFE6;
    border-left-color: #DCDFE6;
  }

  .demo-table-expand {
    font-size: 0;
  }

  .demo-table-expand label {
    width: 90px;
    color: #99a9bf;
  }

  .demo-table-expand .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
    width: 50%;
  }
</style>
