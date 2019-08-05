<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-row :gutter="16">
      <el-col :span="2" style="text-align: center;">
        <el-tooltip content="新增" placement="top">
          <el-button type="primary" icon="el-icon-plus" size="small" circle plain @click="handleCreate">
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="2" style="text-align: center;">
        <el-tooltip content="重置标签页。重置后执行的查询结果将从1号标签页开始" placement="top">
          <el-button type="primary" icon="el-icon-s-home" size="small" circle plain @click="handleResetTabIndex">
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="8">
        <el-input v-model="httpInterfaceQuery.key" size="small" placeholder="输入关键字搜索"
                  style="width: 100%;margin-left: 10px;"/>
      </el-col>
      <el-col :span="4">
        <!--分页-->
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="page1.current"
          :page-sizes="[5, 10, 20, 30, 40, 50]"
          :page-size="page1.size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page1.total"
          background
        >
        </el-pagination>
      </el-col>
    </el-row>
    <!-- 查询结果 -->
    <el-row :gutter="24">
      <el-table
        :data="httpInterfaceData" size="mini" :highlight-current-row="true" cell-class-name="tableCellClass"
        style="width: 100%;" ref="sqlListTable" @row-dblclick="handleRowDbClick"
      >
        <el-table-column
          type="index"
          width="40">
        </el-table-column>
        <el-table-column align="left" width="178px">
          <template slot-scope="scope">
            <el-tooltip content="删除" placement="top">
              <el-button @click="deleteData(scope.$index, scope.row)" size="mini" type="danger"
                         icon="el-icon-delete"
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
            </el-row>
          </template>
        </el-table-column>
      </el-table>
    </el-row>
    <!-- 执行结果 -->
    <el-row style="padding: 0 auto;margin:4px -14px;">
      <el-tabs v-model="currentTabName" type="card">
        <el-tab-pane
          v-for="(item, index) in tabDatas"
          :key="item.name"
          :label="item.title"
          :name="item.name"
        >
          <span slot="label"><i
            v-bind:class="{'el-icon-date': !tabLoading['loading' + (index + 1)], 'el-icon-loading': tabLoading['loading' + (index + 1)]}"
            v-if="item.title == currentTabName"></i>{{item.title}}</span>
          <http-result-panel :value="tableData['data' + index]"></http-result-panel>
        </el-tab-pane>
      </el-tabs>
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
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="请求头">
              <el-row>
                <el-tooltip content="在最后新增一行" placement="top">
                  <el-button type="primary" icon="el-icon-plus" size="mini" circle plain @click="handleAddHeader(-1)">
                  </el-button>
                </el-tooltip>
              </el-row>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <template v-if="temp.httpConfig.method !== 'GET'">
              <el-form-item label="请求体">
                <el-row>
                  <el-tooltip content="格式化" placement="top">
                    <el-button type="primary" icon="el-icon-magic-stick" size="mini" circle plain
                               @click="handleFormatRequestBody">
                    </el-button>
                  </el-tooltip>
                </el-row>
              </el-form-item>
            </template>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <template
              v-if="temp.httpConfig.headers != undefined && temp.httpConfig.headers != null && temp.httpConfig.headers.length > 0">
              <template v-for="(header, idx) in temp.httpConfig.headers">
                <el-row :gutter="24" style="height: 40px;line-height: 40px;">
                  <el-col :span="5" style="text-align: left;">
                    <el-tooltip content="在当前行下面新增一行" placement="top">
                      <el-button type="primary" icon="el-icon-plus" size="mini" circle plain
                                 @click="handleAddHeader(idx)">
                      </el-button>
                    </el-tooltip>
                    <el-button type="primary" icon="el-icon-delete" size="mini" circle plain
                               @click="handleDeleteHeader(idx)">
                    </el-button>
                  </el-col>
                  <el-col :span="9">
                    <el-autocomplete
                      style="width: 100%;"
                      v-model="header.key"
                      :fetch-suggestions="headerKeySearch"
                      placeholder="请输入内容"
                    ></el-autocomplete>
                  </el-col>
                  <el-col :span="1" style="text-align: center;">=</el-col>
                  <el-col :span="8">
                    <el-input v-model="header.value"></el-input>
                  </el-col>
                </el-row>
              </template>
            </template>
            <template v-else>
              &nbsp;
            </template>
          </el-col>
          <el-col :span="12">
            <el-input v-model="temp.httpConfig.body" type="textarea" clearable
                      :autosize='requestBodySize'></el-input>
          </el-col>
        </el-row>
        <template v-if="temp.httpConfig.parameters && temp.httpConfig.parameters.length > 0">
          <!-- 动态参数 -->
          <el-row style="font-size: 18px;">
            参数列表:
          </el-row>
          <el-form-item :label="parameter.name" v-for="(parameter,index) in temp.httpConfig.parameters"
                        :key="'param_' + index">
            <el-input v-model="parameter.label" placeholder="请输入该参数的名称">
              <el-input style="width: 450px;" size="small" v-model="parameter.defaultValue" slot="append"
                        placeholder="默认值. 在列表页面展示时,将默认填充值.">
              </el-input>
            </el-input>
          </el-form-item>
        </template>
        <template v-else>
          &nbsp;
        </template>
      </el-form>
      <div slot="footer">
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
  import {deepClone, isJsonString, resetTemp, formatString} from '@/utils'
  import {confirm, pageParamNames, root} from '@/utils/constants'
  import debounce from 'lodash/debounce'
  import {getParameters} from '@/utils/templateParser'
  import HttpResultPanel from '@/components/panel/HttpResultPanel'

  export default {
    name: 'INTERFACE_HTTP',
    components: {HttpResultPanel},
    data() {
      let maxTabCount = 15;
      let initTabs = function () {
        let result = [];
        for (let i = 0; i < maxTabCount; i++) {
          let title = i + 1 + "";
          result.push({'title': title, 'name': title})
        }
        return result;
      };

      return {
        type: "http",
        storageKey: "INTERFACE_HTTP",
        httpMethods: [
          {"value": 'POST'},
          {"value": 'GET'},
          {"value": 'PUT'},
          {"value": 'DELETE'}
        ],
        requestBodySize: {
          minRows: 10,
          maxRows: 10
        },
        //headers
        commonHeaders: [
          {value: "Accept"},
          {value: "Authorization"},
          {value: "Content-Type"},
          {value: "SOAPAction"}
        ],
        //tabs相关
        currentTabName: '1',
        nextTabName: '1',
        tabDatas: initTabs(),
        tabLoading: {},
        //格式化
        requestBodyFormat: 'json',
        responseBodyFormat: 'json',
        //DB查询列表 相关的变量
        httpInterfaceLoading: false,
        httpInterfaceData: [],
        httpInterfaceQuery: {
          key: '',
          type: "http"
        },
        page1: {
          current: null,
          pages: null,
          size: 10,
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
      this.findHttpInterface();
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
        this.page1.current = 1;//搜索重置页码
        this.findHttpInterface()
      }, 300)
    },//watch
    computed: {},
    methods: {
      headerKeySearch(queryString, cb) {
        let dataList = this.commonHeaders;
        let results = queryString ? dataList.filter(this.createFilter(queryString)) : dataList;
        // 调用 callback 返回建议列表的数据
        cb(results);
      },
      createFilter(queryString) {
        return (item) => {
          return (item.value.toLowerCase().indexOf(queryString.toLowerCase()) >= 0);
        };
      },
      //查询
      findHttpInterface() {
        this.httpInterfaceLoading = true;

        commonConfigApi.queryCommonConfig(this.httpInterfaceQuery, this.page1).then(res => {
          this.httpInterfaceData = res.data.page.records;
          this.httpInterfaceLoading = false;
          pageParamNames.forEach(name => this.$set(this.page1, name, res.data.page[name]))
        })
      },
      //分页
      handleSizeChange(val) {
        this.page1.size = val;
        this.findHttpInterface();
      },
      handleCurrentChange(val) {
        this.page1.current = val;
        this.findHttpInterface();
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
      handleFormatRequestBody() {
        this.temp.httpConfig.body = formatString(this.temp.httpConfig.body);
      },
      handleResetTabIndex() {
        if (this.tableData.data0 != undefined) {
          this.tableData.data0 = undefined;
        }
        this.currentTabName = '1';
        this.nextTabName = '1';
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
        let _parameters = row.httpConfig.parameters;
        if (_parameters !== null && _parameters.length > 0) {
          this.$refs['sqlListTable'].toggleRowExpansion(row);
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
          let tempData = this.temp//copy obj
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
          return null;
        }
        for (let i = 0; i < arr.length; i++) {
          if (parameterName === arr[i].name) {
            return arr[i];
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
        //生成新的参数对象
        let newParameterObj = parameters ? parameters.reduce((obj, currentValue) => {
          obj[currentValue] = {
            name: currentValue,
            label: null,
            defaultValue: null
          };
          return obj;
        }, {}) : {};
        //原有参数
        let oldParameters = tempEntity.httpConfig.parameters;
        tempEntity.httpConfig.parameters = [];//重置
        //参数去重
        let oldParameterObj = oldParameters ? oldParameters.reduce((obj, currentValue) => {
          obj[currentValue.name] = currentValue;
          return obj;
        }, {}) : undefined;
        //重置 并使用新的参数信息
        tempEntity.httpConfig.parameters = [];
        let idx = 0;
        for (let parameterName in newParameterObj) {
          if (oldParameterObj && oldParameterObj[parameterName]) {
            tempEntity.httpConfig.parameters.splice(idx, 0, oldParameterObj[parameterName]);
          } else {
            //新的参数
            tempEntity.httpConfig.parameters.splice(idx, 0, {
              name: parameterName,
              label: parameterName,
              defaultValue: null
            });
          }
          idx++;
        }
      },
      executeData(idx, row) {
        //设置条件lading
        this.$set(this.tableData, "loading" + idx, true);
        //设置当前标签页
        this.currentTabName = this.nextTabName;
        //设置当前标签页的loading
        this.$set(this.tabLoading, "loading" + parseInt(this.currentTabName), true);
        //当前标签页数据置空
        this.$set(this.tableData, "data" + (parseInt(this.currentTabName) - 1), null);
        //验证参数是否为空
        let _parameters = row.httpConfig.parameters;
        if (_parameters !== null && _parameters.length > 0) {
          //有参数, 则一定展开
          this.$refs['sqlListTable'].toggleRowExpansion(row, true);
          for (let i in _parameters) {
            if (_parameters[i].defaultValue === undefined || _parameters[i].defaultValue === null || _parameters[i].defaultValue.trim() === '') {
              this.$message.warning('参数[' + _parameters[i].label + "]不能为空");
              this.$set(this.tableData, "loading" + idx, false);
              this.$set(this.tabLoading, "loading" + parseInt(this.currentTabName), false);
              return;
            }
          }
        }
        httpApi.execute(row).then(res => {
          let data = res.data.data;

          data.request.body = formatString(data.request.body);
          data.response.body = formatString(data.response.body);
          //计算下个标签页
          this.nextTabName = parseInt(this.currentTabName) + 1 + "";
          if (parseInt(this.nextTabName) > this.tabDatas.length) {
            this.nextTabName = '1';//循环 超过则从1开始
          }

          this.$set(this.tableData, "data" + (parseInt(this.currentTabName) - 1), data);
          this.$set(this.tableData, "loading" + idx, false);
          this.$set(this.tabLoading, "loading" + parseInt(this.currentTabName), false);
        }).catch(e => {
          console.log(e);
          //当前标签页数据置空
          this.$set(this.tableData, "data" + (parseInt(this.currentTabName) - 1), null);
          this.$set(this.tableData, "loading" + idx, false);
          this.$set(this.tabLoading, "loading" + parseInt(this.currentTabName), false);
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
  .el-form-item {
    margin-bottom: 4px;
  }

  .el-dialog__body {
    padding: 4px 20px;
    color: #606266;
    font-size: 14px;
    word-break: break-all;
  }
</style>
<style rel="stylesheet/scss" lang="scss">
  .el-select .el-input {
    width: 130px;
  }

  .el-table__expanded-cell[class*=cell] {
    padding: 4px 10px;
  }
</style>
