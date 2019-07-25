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
            <template v-if="props.row.httpConfig.parameters && props.row.httpConfig.parameters.length > 0">
              <el-form inline>
                <el-form-item v-for="(param, index) in props.row.httpConfig.parameters"
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
              <el-input v-model="tableData['data' + props.$index]" type="textarea"></el-input>
            </template>
          </template>
        </el-table-column>
        <el-table-column
          type="index"
          width="30">
        </el-table-column>
        <el-table-column align="right" width="140px">
          <template slot-scope="scope">
            <el-tooltip content="删除" placement="top">
              <el-button @click="deleteData(scope.$index, scope.row)" size="mini" type="danger" icon="el-icon-delete"
                         circle plain></el-button>
            </el-tooltip>
            <el-tooltip content="编辑" placement="top">
              <el-button @click="handleEdit(scope.$index, scope.row)" size="mini" type="info" icon="el-icon-edit"
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
    <!--弹出窗口：新增/编辑-->
    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="80%">
      <el-form :rules="rules" ref="dataForm" :model="temp" label-position="left" label-width="120px">
        <el-form-item label="请求名称">
          <el-input v-model="temp.id" v-show="false"></el-input>
          <el-input v-model="temp.name"></el-input>
        </el-form-item>
        <el-form-item label="请求URL">
          <el-input placeholder="请输入请求URL" v-model="temp.httpConfig.url" class="input-with-select">
            <el-select v-model="temp.httpConfig.method" slot="prepend" placeholder="请选择">
              <el-option
                v-for="httpMethod in httpMethods"
                :key="httpMethod.value"
                :label="httpMethod.value"
                :value="httpMethod.value">
              </el-option>
            </el-select>
          </el-input>
        </el-form-item>
        <el-row :gutter="24" style="height: 300px;">
          <el-col :span="12">
            <el-form-item label="请求头">
              <el-row>
                <el-tooltip content="在最后新增一行" placement="top">
                  <el-button type="primary" icon="el-icon-plus" size="mini" circle plain @click="handleAddHeader(-1)">
                  </el-button>
                </el-tooltip>
              </el-row>
              <el-row>
                <template v-for="(header, idx) in temp.httpConfig.headers" v-if="temp.httpConfig.headers.length > 0">
                  <el-col class="line" :span="6" style="text-align: left;">
                    <el-button type="primary" icon="el-icon-plus" size="mini" circle plain
                               @click="handleAddHeader(idx)">
                    </el-button>
                    <el-button type="primary" icon="el-icon-delete" size="mini" circle plain
                               @click="handleDeleteHeader(idx)">
                    </el-button>
                  </el-col>
                  <el-col :span="8">
                    <el-input v-model="header.key"></el-input>
                  </el-col>
                  <el-col class="line" :span="2" style="text-align: center;">=</el-col>
                  <el-col :span="8">
                    <el-input v-model="header.value"></el-input>
                  </el-col>
                </template>
              </el-row>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求体">
              <el-input v-model="temp.httpConfig.body" type="textarea" clearable></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <template v-if="temp.httpConfig.parameters && temp.httpConfig.parameters.length > 0">
          <!-- 动态参数 -->
          <el-row style="font-size: 18px;">
            参数列表:
          </el-row>
          <el-form-item :label="parameter.name" v-for="(parameter,index) in temp.httpConfig.parameters"
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
  import httpApi from '@/api/operate/httpApi'
  import {parseTime, resetTemp, isJsonString} from '@/utils'
  import {confirm, pageParamNames, root} from '@/utils/constants'
  import debounce from 'lodash/debounce'
  import {getParameters} from '@/utils/templateParser'

  export default {
    name: 'INTERFACE_HTTP',
    components: {},
    data() {
      return {
        type: "http",
        storageKey: "INTERFACE_HTTP",
        httpMethods: [
          {"value": 'POST'},
          {"value": 'GET'},
          {"value": 'PUT'},
          {"value": 'DELETE'}
        ],
        //方法
        isJsonString: isJsonString,
        getParameters: getParameters,
        //DB查询列表 相关的变量
        dbQueryNamesLoading: false,
        dbQueryNamesData: [],
        dbQueryNamesQuery: {
          key: null,
          type: "http"
        },
        dbQueryNamesPage: {
          current: null,
          pages: null,
          size: null,
          total: null
        },
        //查询结果相关
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
          httpConfig: {
            url: '',
            method: '',
            headers: [],
            body: '',
            parameters: []
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
      let tempDataStr = sessionStorage.getItem(this.storageKey);
      // 在create后还原数据, 实现页面数据状态保存
      console.log(tempDataStr)
      if (tempDataStr) {
        let tempData = JSON.parse(sessionStorage.getItem(this.storageKey));
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
      sessionStorage.setItem(this.storageKey, JSON.stringify(this._data));
      console.log(sessionStorage.getItem(this.storageKey));
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
      handleAddHeader(idx) {
        if (idx == -1) {
          if (this.temp.httpConfig.headers != null) {
            length = this.temp.httpConfig.headers.length;
            this.temp.httpConfig.headers.splice(length, 1, {key: "", value: ""});
          } else {
            this.temp.httpConfig.headers = [];
            this.temp.httpConfig.headers.splice(length, 0, {key: "", value: ""});
          }
        } else {
          this.temp.httpConfig.headers.splice(idx + 1, 0, {key: "", value: ""});
        }
      },
      handleDeleteHeader(idx) {
        this.temp.httpConfig.headers.splice(idx, 1);
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
        this.temp = sqlEntity;
        this.dialogStatus = 'update';
        this.dialogFormVisible = true;
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
        const sql = sqlEntity.httpConfig.sqlTemplate;
        let parameters = getParameters(sql);
        let oldParameters = sqlEntity.httpConfig.parameters;

        sqlEntity.httpConfig.parameters = [];//重置
        for (let idx in parameters) {
          sqlEntity.httpConfig.parameters.splice(idx, 1, {
            name: parameters[idx],
            label: this.getParameterLabel(oldParameters, parameters[idx])
          })
        }
      },
      executeData(idx, row) {
        //展开
        this.$refs['sqlListTable'].toggleRowExpansion(row, true);
        //验证参数是否为空
        let _parameters = row.httpConfig.parameters;
        if (_parameters !== null && _parameters.length > 0) {
          for (let i in _parameters) {
            if (_parameters[i].defaultValue === undefined || _parameters[i].defaultValue === null || _parameters[i].defaultValue.trim() === '') {
              this.$message.warning('参数[' + _parameters[i].label + "]不能为空");
              return;
            }
          }
        }
        httpApi.execute(row).then(res => {
          let data = res.data.data;
          let listColumns = [];
          if (data && data.length > 0) {
            listColumns = Object.keys(data[0]);
          }
          this.$set(this.tableData, "data" + idx, data);
          this.$set(this.tableData, "loading" + idx, false);
        }).catch(e => {
          this.$set(this.tableData, "loading" + idx, false);
        });
      },
    }
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
