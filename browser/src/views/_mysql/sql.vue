<template>
  <div class="app-container">
    <el-row>
      <el-tooltip content="新增" placement="top">
        <el-button type="primary" icon="el-icon-plus" size="mini" circle plain @click="handleCreate">
        </el-button>
      </el-tooltip>
      <el-input v-model="dbQueryNamesQuery.key" size="mini" placeholder="输入关键字搜索" style="width: 80%;"/>
    </el-row>
    <el-row :gutter="24">
      <el-table
        :data="dbQueryNamesData"
        style="width: 100%;" ref="sqlListTable" @row-dblclick="handleRowDbClick">
        <el-table-column type="expand">
          <template slot-scope="props">
            <!-- 条件面板 -->
            <template v-if="props.row.dbQueryConfig.parameters && props.row.dbQueryConfig.parameters.length > 0">
              <el-form inline>
                <el-form-item v-for="(param, index) in props.row.dbQueryConfig.parameters"
                              :key="'param' + props.$index + '_' + index">
                  <el-tooltip class="item" effect="dark" :content="param.label" placement="right-start">
                    <el-input v-model="param.defaultValue" :placeholder="param.label"
                              style="margin: 4px;"></el-input>
                  </el-tooltip>
                </el-form-item>
              </el-form>
            </template>
            <!-- 结果面板 -->
            <template v-if="tableData['data' + props.$index]">
              <el-table style="width: 100%;height: 95%;"
                        :data="tableData['data' + props.$index]"
                        v-loading.body="tableData['loading' + props.$index]"
                        element-loading-text="加载中"
                        border fit highlight-current-row
                        @cell-dblclick="handleCellDbClick"
              >
                <el-table-column :prop="column" :label="column"
                                 v-for="(column,idx) in tableData['columns' + props.$index]"
                                 :show-overflow-tooltip='true' :key="idx">
                  <template slot-scope="scope">
                    <span v-text="scope.row[column]"></span>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </template>
        </el-table-column>
        <el-table-column
          type="index"
          width="30">
        </el-table-column>
        <el-table-column align="left" width="178px">
          <template slot-scope="scope">
            <el-tooltip content="删除" placement="top">
              <el-button @click="deleteData(scope.$index, scope.row)" size="mini" type="danger" icon="el-icon-delete"
                         circle plain></el-button>
            </el-tooltip>
            <el-tooltip content="编辑" placement="top">
              <el-button @click="handleEdit(scope.$index, scope.row)" size="mini" type="info" icon="el-icon-edit"
                         circle plain></el-button>
            </el-tooltip>
            <el-tooltip content="复制" placement="top">
              <el-button @click="handleCopy(scope.$index, scope.row)" size="mini" type="info"
                         icon="el-icon-document-copy"
                         circle plain></el-button>
            </el-tooltip>
            <el-tooltip content="执行" placement="top">
              <el-button @click="executeData(scope.$index, scope.row)" size="mini" type="info"
                         icon="el-icon-caret-right" circle plain></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column
          label="名称"
          prop="name">
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
        <el-form-item label="名称" prop="name">
          <el-input v-model="temp.id" v-show="false"></el-input>
          <el-input v-model="temp.name"></el-input>
        </el-form-item>
        <!-- 修改时不可更改数据源 -->
        <el-form-item label="数据源">
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
        <el-form-item label="SQL">
          <mysql-editor v-model="temp.dbQueryConfig.sqlTemplate"></mysql-editor>
        </el-form-item>
        <template v-if="temp.dbQueryConfig.parameters && temp.dbQueryConfig.parameters.length > 0">
          <!-- 动态参数 -->
          <el-row style="font-size: 18px;">
            参数列表:
          </el-row>
          <el-form-item :label="parameter.name" v-for="(parameter,index) in temp.dbQueryConfig.parameters"
                        :key="'param_' + index">
            <el-input v-model="parameter.label" placeholder="请输入该参数的名称"></el-input>
          </el-form-item>
        </template>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="generateParameter()">生成参数</el-button>
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
  import {parseTime, resetTemp, isJsonString, deepClone} from '@/utils'
  import {confirm, pageParamNames, root} from '@/utils/constants'
  import debounce from 'lodash/debounce'
  import JsonEditor from "../../components/JsonEditor/index";
  import MysqlEditor from "../../components/MysqlEditor/index";
  import {getParameters} from '@/utils/templateParser'

  export default {
    name: 'MySQL_SQL',
    components: {JsonEditor, MysqlEditor},
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
        storageKey: "MySQL_SQL",
        //方法
        isJsonString: isJsonString,
        getParameters: getParameters,
        //DB查询列表 相关的变量
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
        tableData: {},
        //格式化相关
        needFormatValue: null,
        formatDialogVisible: false,
        textFormat: null,
        //弹出框及新增和修改相关
        dialogFormVisible: false,
        dialogStatus: '',
        temp: {
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
    },
    mounted() {
    },
    beforeDestroy() {

    },
    destroyed() {
    },

    watch: {
      //延时查询
      'dbQueryNamesQuery.key': debounce(function () {
        this.findDbQueryNames()
      }, 300)
    },//watch
    computed: {},
    methods: {
      //新增
      //数据库查询语句的相关操作
      findDbQueryNames() {
        if (this.dbQueryNamesQuery.key !== '') {
          this.dbQueryNamesLoading = true;

          commonConfigApi.queryCommonConfig(this.dbQueryNamesQuery, this.dbQueryNamesPage).then(res => {
            this.dbQueryNamesData = res.data.page.records;
            this.dbQueryNamesLoading = false
            this.tableData = {};
          })
        } else {
          this.dbQueryNamesData = [];
          this.tableData = {};
        }
      },
      handleCellDbClick(row, column, cell, event) {
        let str = row[column.property];
        if (str === undefined || str === '') {
          return;
        }
        if (isJsonString(str)) {
          this.textFormat = 'json';
          this.formatDialogVisible = true;
          this.needFormatValue = JSON.parse(row[column.property]);
          return;
        }
        this.textFormat = 'text';
        this.formatDialogVisible = true;
        this.needFormatValue = str;
      },
      handleCreate() {
        resetTemp(this.temp)
        this.dialogStatus = 'create'
        this.dialogFormVisible = true
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        })
      },
      handleEdit(idx, sqlEntity) {
        this.temp = deepClone(sqlEntity);
        this.dialogStatus = 'update';
        this.dialogFormVisible = true;
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        })
      },
      handleCopy(idx, sqlEntity) {
        this.temp = deepClone(sqlEntity);
        this.dialogStatus = 'update';
        this.dialogFormVisible = create;
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        })
      },
      handleRowDbClick(row) {
        this.$refs['sqlListTable'].toggleRowExpansion(row);
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
      updateData() {
        this.$refs['dataForm'].validate((valid) => {
          if (!valid) {
            return;
          }
          let tempData = Object.assign({}, this.temp)//copy obj
          tempData.type = this.type;
          commonConfigApi.updateCommonConfig(tempData).then((res) => {
            this.dialogFormVisible = false;
            this.$message.success("保存成功");
          })
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
      getParameterLabel(arr, parameterName) {
        if (!arr) {
          return '';
        }
        for (let i = 0; i < arr.length; i++) {
          if (parameterName === arr[i].name) {
            return arr[i].label;
          }
        }
      },
      generateParameter() {
        const sqlEntity = this.temp;
        const sql = sqlEntity.dbQueryConfig.sqlTemplate;
        let parameters = getParameters(sql);
        let oldParameters = sqlEntity.dbQueryConfig.parameters;

        sqlEntity.dbQueryConfig.parameters = [];//重置
        for (let idx in parameters) {
          sqlEntity.dbQueryConfig.parameters.splice(idx, 1, {
            name: parameters[idx],
            label: this.getParameterLabel(oldParameters, parameters[idx])
          })
        }
      },
      executeData(idx, row) {
        //展开
        this.$refs['sqlListTable'].toggleRowExpansion(row, true);
        //验证参数是否为空
        let _parameters = row.dbQueryConfig.parameters;
        if (_parameters !== null && _parameters.length > 0) {
          for (let i in _parameters) {
            if (_parameters[i].defaultValue === undefined || _parameters[i].defaultValue === null || _parameters[i].defaultValue.trim() === '') {
              this.$message.warning('参数[' + _parameters[i].label + "]不能为空");
              return;
            }
          }
        }
        dbOperateApi.execute(row).then(res => {
          let data = res.data.data;
          let listColumns = [];
          if (data && data.length > 0) {
            listColumns = Object.keys(data[0]);
          }
          this.$set(this.tableData, "data" + idx, data);
          this.$set(this.tableData, "loading" + idx, false);
          this.$set(this.tableData, "columns" + idx, listColumns);
        }).catch(e => {
          this.$set(this.tableData, "loading" + idx, false);
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
</style>
<style rel="stylesheet/scss" lang="scss">
  .el-table__expanded-cell[class*=cell] {
    padding: 4px 10px;
  }
</style>
