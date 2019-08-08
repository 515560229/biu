<template>
  <div class="app-container">
    <el-row>
      <el-tooltip content="新增" placement="top">
        <el-button type="primary" icon="el-icon-plus" size="mini" circle plain @click="handleCreate">
        </el-button>
      </el-tooltip>
      <el-input v-model="httpInterfaceQuery.key" size="mini" placeholder="输入关键字搜索" style="width: 80%;"/>
    </el-row>
    <el-row :gutter="24">
      <el-table
        :data="httpInterfaceData"
        style="width: 100%;" ref="sqlListTable" @row-dblclick="handleRowDbClick" @expand-change="handleExpandChange">
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
              <el-button @click="handleEdit(scope.$index, scope.row)"
                         size="mini" type="info"
                         icon="el-icon-edit"
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
        <el-table-column type="expand">
          <template slot-scope="props">
            <el-row v-loading="tableData['loading' + props.$index]"
                    element-loading-text="拼命加载中"
                    element-loading-spinner="el-icon-loading"
                    element-loading-background="rgba(0, 0, 0, 0.2)"
                    style="border: 1px solid #91bbe9;"
            >
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
                <el-row>
                  <el-row :gutter="24">
                    <el-col :span="12">
                      <template v-if="tableData['data' + props.$index].response.statusCodeValue === 200">
                        <el-tag type="success">{{tableData['data' + props.$index].request.url}}</el-tag>
                      </template>
                      <template v-else>
                        <el-tag type="danger">{{tableData['data' + props.$index].request.url}}</el-tag>
                      </template>
                    </el-col>
                    <el-col :span="12" style="text-align: right;">
                      <template v-if="tableData['data' + props.$index].response.statusCodeValue === 200">
                        <el-tag type="success">{{tableData['data' +
                          props.$index].response.statusCodeValue}}/{{tableData['data' +
                          props.$index].response.statusCode}}
                        </el-tag>
                      </template>
                      <template v-else>
                        <el-tag type="danger">{{tableData['data' +
                          props.$index].response.statusCodeValue}}/{{tableData['data' +
                          props.$index].response.statusCode}}
                        </el-tag>
                      </template>
                    </el-col>
                  </el-row>
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
                        <el-row v-for="(value, name) in tableData['data' + props.$index].request.headers" :key="name">
                          {{name}}:{{value}}
                        </el-row>
                      </el-row>
                    </el-col>
                    <el-col :span="12">
                      <el-row style="padding-left: 20px;">
                        <el-row v-for="(value, name) in tableData['data' + props.$index].response.headers" :key="name">
                          {{name}}:{{value}}
                        </el-row>
                      </el-row>
                    </el-col>
                  </el-row>
                  <el-row :gutter="24">
                    <el-col :span="12">
                      <template v-if="tableData['data' + props.$index].request.body != null">
                        requestBody:
                        <el-row style="padding: 0 20px;">
                          <el-input type="textarea" :autosize='requestBodySize'
                                    v-model="tableData['data' + props.$index].request.body" readonly></el-input>
                        </el-row>
                      </template>
                      <template v-else>
                        &nbsp;
                      </template>
                    </el-col>
                    <el-col :span="12">
                      <template v-if="tableData['data' + props.$index].response.body != null">
                        responseBody:
                        <el-row style="padding: 0 20px;">
                          <el-input type="textarea" :autosize='requestBodySize'
                                    v-model="tableData['data' + props.$index].response.body" readonly></el-input>
                        </el-row>
                      </template>
                      <template v-else>
                        &nbsp;
                      </template>
                    </el-col>
                  </el-row>
                </el-row>
              </template>
            </el-row>
          </template>
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
            <el-select v-model="temp.httpConfig.method" slot="prepend" placeholder="请选择" style="width:150px">
              <el-option
                v-for="httpMethod in httpMethods"
                :key="httpMethod.value"
                :label="httpMethod.value"
                :value="httpMethod.value">
              </el-option>
            </el-select>
          </el-input>
        </el-form-item>
        <el-row :gutter="24">
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
                    <el-tooltip content="在当前行下面新增一行" placement="top">
                      <el-button type="primary" icon="el-icon-plus" size="mini" circle plain
                                 @click="handleAddHeader(idx)">
                      </el-button>
                    </el-tooltip>
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
            <template v-if="temp.httpConfig.method !== 'GET'">
              <el-form-item label="请求体">
                <el-input v-model="temp.httpConfig.body" type="textarea" clearable
                          :autosize='requestBodySize'></el-input>
              </el-form-item>
            </template>
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
        <el-button v-if="dialogStatus==='create'" type="primary" @click="createData">创建</el-button>
        <el-button v-else type="primary" @click="updateData">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>

  import commonConfigApi from '@/api/config/commonConfig'
  import httpApi from '@/api/operate/httpApi'
  import {deepClone, isJsonString, resetTemp} from '@/utils'
  import {confirm} from '@/utils/constants'
  import debounce from 'lodash/debounce'
  import {getParameters} from '@/utils/templateParser'
  //https://github.com/vkiryukhin/pretty-data
  let pd = require('pretty-data').pd;

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
        //格式化
        requestBodyFormat: 'json',
        responseBodyFormat: 'json',
        //方法
        isJsonString: isJsonString,
        getParameters: getParameters,
        //DB查询列表 相关的变量
        httpInterfaceLoading: false,
        httpInterfaceData: [],
        httpInterfaceQuery: {
          key: null,
          type: "http"
        },
        page1: {
          current: null,
          pages: null,
          size: 50,
          total: null
        },
        //查询结果相关
        tableData: {},
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
    },
    mounted() {
    },
    beforeDestroy() {

    },
    destroyed() {
    },
    //keep-alive钩子函数：组件消失，被缓存时调用
    deactivated() {
    },
    watch: {
      //延时查询
      'httpInterfaceQuery.key': debounce(function () {
        this.findHttpInterface()
      }, 300)
    },//watch
    computed: {},
    methods: {
      //新增
      //数据库查询语句的相关操作
      findHttpInterface() {
        if (this.httpInterfaceQuery.key !== '') {
          this.httpInterfaceLoading = true;

          commonConfigApi.queryCommonConfig(this.httpInterfaceQuery, this.page1).then(res => {
            this.httpInterfaceData = res.data.page.records;
            this.httpInterfaceLoading = false
            this.tableData = {};
          })
        } else {
          this.httpInterfaceData = [];
          this.tableData = {};
        }
      },
      handleAddHeader(idx) {
        if (idx === -1) {
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
      handleCreate() {
        resetTemp(this.temp)
        this.dialogStatus = 'create'
        this.dialogFormVisible = true
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        })
      },
      handleEdit(idx, sqlEntity) {
        console.log("handleEdit")
        this.temp = deepClone(sqlEntity);
        this.dialogStatus = 'update';
        this.dialogFormVisible = true;
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        })
      },
      handleCopy(idx, sqlEntity) {
        console.log("handleEditByCopy")
        this.temp = deepClone(sqlEntity);
        this.temp.id = null;
        this.dialogStatus = 'create';
        this.dialogFormVisible = true;
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        })
      },
      handleRowDbClick(row) {
        this.$refs['sqlListTable'].toggleRowExpansion(row);
      },
      /**
       * 如果是展开某行, 则将其它行折叠
       * @param row
       * @param expandRows
       */
      handleExpandChange(row, expandRows) {
        let rowId = row.id;
        let open = false;
        if (expandRows != null) {
          for (let i = 0; i < expandRows.length; i++) {
            if (rowId === expandRows[i].id) {
              open = true;
            }
          }
        }
        if (open) {
          if (expandRows != null) {
            for (let i = 0; i < expandRows.length; i++) {
              if (rowId !== expandRows[i].id) {
                this.$refs['sqlListTable'].toggleRowExpansion(expandRows[i], false);
              }
            }
          }
        }
      },
      createData() {
        this.generateParameter();
        this.$refs['dataForm'].validate((valid) => {
          if (!valid) {
            return;
          }
          let tempData = Object.assign({}, this.temp)//copy obj
          tempData.type = this.type;
          commonConfigApi.addCommonConfig(tempData).then((res) => {
            this.temp = res.data.data;
            this.dialogFormVisible = false;
            this.findHttpInterface();
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
            this.findHttpInterface();
            this.$message.success("保存成功");
          })
        });
      },
      deleteData(idx, httpInterface) {
        this.$confirm('您确定要永久删除该查询语句？', '提示', confirm).then(() => {
          commonConfigApi.deleteCommonConfig(httpInterface.id).then(res => {
            this.$message.success("删除成功");
            this.httpInterfaceData.splice(idx, 1);
          })
        }).catch(() => {
          this.$message.info("已取消删除")
        });
      },
      getParameter(arr, parameterName) {
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
        const tempEntity = this.temp;
        const url = tempEntity.httpConfig.url;
        //url的参数
        let parameters = getParameters(url);
        //headers的参数
        let headers = tempEntity.httpConfig.headers;
        if (headers != null) {
          for (let i = 0; i < headers.length; i++) {
            let tempHeaderKey = getParameters(headers[i].key);
            if (tempHeaderKey != null && tempHeaderKey.length > 0) {
              for (let j = 0; j < tempHeaderKey.length; j++) {
                parameters.splice(parameters.length, 0, tempHeaderKey[j]);
              }
            }
            let tempHeaderValue = getParameters(headers[i].value);
            if (tempHeaderValue != null && tempHeaderValue.length > 0) {
              for (let j = 0; j < tempHeaderValue.length; j++) {
                parameters.splice(parameters.length, 0, tempHeaderValue[j]);
              }
            }
          }
        }
        //请求体的参数
        if (tempEntity.httpConfig.body != null) {
          let tempBodyParameters = getParameters(tempEntity.httpConfig.body);
          if (tempBodyParameters != null && tempBodyParameters.length > 0) {
            for (let j = 0; j < tempBodyParameters.length; j++) {
              parameters.splice(parameters.length, 0, tempBodyParameters[j]);
            }
          }
        }

        let oldParameters = tempEntity.httpConfig.parameters;

        tempEntity.httpConfig.parameters = [];//重置
        for (let idx in parameters) {
          let label = this.getParameter(oldParameters, parameters[idx]);
          if (label === null || label.trim() === '') {
            label = parameters[idx];
          }
          tempEntity.httpConfig.parameters.splice(idx, 1, {
            name: parameters[idx],
            label: label
          });
        }
      },
      executeData(idx, row) {
        this.$set(this.tableData, "loading" + idx, true);
        //展开
        this.$refs['sqlListTable'].toggleRowExpansion(row, true);
        //验证参数是否为空
        let _parameters = row.httpConfig.parameters;
        if (_parameters !== null && _parameters.length > 0) {
          for (let i in _parameters) {
            if (_parameters[i].defaultValue === undefined || _parameters[i].defaultValue === null || _parameters[i].defaultValue.trim() === '') {
              this.$message.warning('参数[' + _parameters[i].label + "]不能为空");
              this.$set(this.tableData, "loading" + idx, false);
              return;
            }
          }
        }
        httpApi.execute(row).then(res => {
          let data = res.data.data;

          data.request.body = formatString(data.request.body);
          data.response.body = formatString(data.response.body);

          this.$set(this.tableData, "data" + idx, data);
          this.$set(this.tableData, "loading" + idx, false);
        }).catch(e => {
          console.log(e);
          this.$set(this.tableData, "loading" + idx, false);
        });
      },
      formatter(val) {
        if (isJsonString(val)) {
          return JSON.stringify(JSON.parse(val), null, 4);
        }
        return val;
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
