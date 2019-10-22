<template>
  <div class="app-container">
    <el-row :gutter="16">
      <el-col :span="2" style="text-align: center;">
        <el-tooltip content="新增" placement="top">
          <el-button type="primary" icon="el-icon-plus" size="mini" circle plain @click="handleCreate">
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="2" style="text-align: center;">
        <el-tooltip content="重置标签页。重置后执行的查询结果将从1号标签页开始" placement="top">
          <el-button type="primary" icon="el-icon-s-home" size="mini" circle plain @click="handleResetTabIndex">
          </el-button>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-input v-model="producerNamesQuery.key" size="mini" placeholder="输入关键字搜索" style="width: 80%;"/>
      </el-col>
      <el-col :span="6">
        <!--分页-->
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="page1.current"
          :page-sizes="[5, 10, 20, 30, 40, 50]"
          :page-size="page1.size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page1.total"
        >
        </el-pagination>
      </el-col>
    </el-row>
    <el-row :gutter="24">
      <el-table
        :data="producerNamesData"
        style="width: 100%;" ref="sqlListTable" @row-dblclick="handleRowDbClick">
        <el-table-column
          type="index"
          width="40">
        </el-table-column>
        <el-table-column align="left" width="178px">
          <template slot-scope="scope">
            <el-tooltip content="删除" placement="top">
              <el-button @click="deleteData(scope.$index, scope.row)" size="mini" type="danger" icon="el-icon-delete"
                         :disabled="scope.row.creator === undefined || scope.row.creator === null || (currentUser !== 'admin' && scope.row.creator !== currentUser)"
                         circle plain></el-button>
            </el-tooltip>
            <el-tooltip content="编辑" placement="top">
              <el-button @click="handleEdit(scope.$index, scope.row)" size="mini" type="info" icon="el-icon-edit"
                         :disabled="scope.row.creator === undefined || scope.row.creator === null || (currentUser !== 'admin' && scope.row.creator !== currentUser)"
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
            <!-- 条件面板 -->
            <template v-if="props.row.producerConfig.parameters && props.row.producerConfig.parameters.length > 0">
              <el-form inline>
                <el-form-item v-for="(param, index) in props.row.producerConfig.parameters"
                              :key="'param' + props.$index + '_' + index">
                  <el-tooltip class="item" effect="dark" :content="param.label" placement="right-start">
                    <el-input v-model="param.defaultValue" :placeholder="param.label"
                              style="margin: 4px;"></el-input>
                  </el-tooltip>
                </el-form-item>
              </el-form>
            </template>
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
          <kafka-producer-result-panel :value="tableData['val' + index]"
                                       :v-loading="tableData['loading' + index]"></kafka-producer-result-panel>
        </el-tab-pane>
      </el-tabs>
    </el-row>
    <!--弹出窗口：新增/编辑-->
    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="80%">
      <el-form :rules="rules" ref="dataForm" :model="temp" label-position="left" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="temp.id" v-show="false"></el-input>
          <el-input v-model="temp.name"></el-input>
        </el-form-item>
        <!-- 修改时不可更改数据源 -->
        <el-form-item label="主题">
          <el-select v-model="temp.producerConfig.topic" filterable remote reserve-keyword
                     placeholder="请输入主题名称,支持模糊搜索"
                     :remote-method="findTopic"
                     :loading="topicNamesLoading"
                     style="width: 100%"
          >
            <el-option
              v-for="item in topicNames"
              :key="item.name"
              :label="item.name"
              :value="item.name"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="消息内容" prop="message">
          <el-input v-model="temp.producerConfig.message" type="textarea" :autosize="cellDetailsSize"></el-input>
        </el-form-item>
        <template v-if="temp.producerConfig.parameters && temp.producerConfig.parameters.length > 0">
          <!-- 动态参数 -->
          <el-row style="font-size: 18px;">
            参数列表:
          </el-row>
          <el-form-item :label="parameter.name" v-for="(parameter,index) in temp.producerConfig.parameters"
                        :key="'param_' + index">
            <el-input v-model="parameter.label" placeholder="请输入该参数的名称">
              <el-input style="width: 450px;" size="small" v-model="parameter.defaultValue" slot="append"
                        placeholder="默认值. 在列表页面展示时,将默认填充值.">
              </el-input>
            </el-input>
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

  import {mapState} from 'vuex'
  import commonConfigApi from '@/api/config/commonConfig'
  import kafkaApi from '@/api/operate/kafkaApi'
  import {parseTime, resetTemp, isJsonString, deepClone} from '@/utils'
  import {confirm, pageParamNames, root} from '@/utils/constants'
  import debounce from 'lodash/debounce'
  import {getParameters} from '@/utils/templateParser'
  import KafkaProducerResultPanel from '@/components/panel/KafkaProducerResultPanel'

  export default {
    name: 'KafkaProducer',
    components: {KafkaProducerResultPanel},
    data() {

      let validateNotNull = (rule, value, callback) => {
        if (this.dialogStatus == 'create' && value === '') {
          callback(new Error('必填'));
        } else {
          callback();
        }
      };
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
        type: "producer",
        cellDetailsSize: {
          minRows: 16,
          maxRows: 16
        },
        //DB查询列表 相关的变量
        producerNamesLoading: false,
        producerNamesData: [],
        producerNamesQuery: {
          key: '',
          type: "producer"
        },
        page1: {
          current: null,
          pages: null,
          size: 50,
          total: null
        },
        //tabs相关
        currentTabName: '1',
        nextTabName: '1',
        tabDatas: initTabs(),
        tabLoading: {},
        //数据源查询相关变量
        topicNames: [],
        topicNamesLoading: false,
        topicNamesPage: {
          current: null,
          pages: null,
          size: null,
          total: null
        },
        topicNamesQuery: {
          key: '',
          type: "kafka"
        },
        //查询结果相关
        tableData: [],
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
          producerConfig: {
            topic: null,
            message: null,
            broker: null,
            version: null,
            clusterName: null,
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
      this.findProducerNames();
    },
    beforeDestroy() {

    },
    destroyed() {
    },

    watch: {
      //延时查询
      'producerNamesQuery.key': debounce(function () {
        this.page1.current = 1;//搜索重置页码
        this.findProducerNames();
      }, 300)
    },//watch
    computed: {
      ...mapState({
        currentUser: function (state) {
          return state.user.name;
        }
      })
    },
    methods: {
      //新增
      //数据库查询语句的相关操作
      findProducerNames() {
        this.producerNamesLoading = true;
        commonConfigApi.queryCommonConfig(this.producerNamesQuery, this.page1).then(res => {
          this.producerNamesData = res.data.page.records;
          this.producerNamesLoading = false
          this.tableData = {};
          pageParamNames.forEach(name => this.$set(this.page1, name, res.data.page[name]))
        })
      },
      //分页
      handleSizeChange(val) {
        this.page1.size = val;
        this.findProducerNames();
      },
      handleCurrentChange(val) {
        this.page1.current = val;
        this.findProducerNames();
      },
      handleCreate() {
        resetTemp(this.temp);
        this.dialogStatus = 'create';
        this.dialogFormVisible = true;
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        });
        this.findTopic();
      },
      handleEdit(idx, entity) {
        this.temp = deepClone(entity);
        this.dialogStatus = 'update';
        this.dialogFormVisible = true;
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        })
        this.findTopic(this.temp.producerConfig.topic);
      },
      handleCopy(idx, sqlEntity) {
        this.temp = deepClone(sqlEntity);
        this.dialogStatus = 'create';
        this.dialogFormVisible = true;
        this.$nextTick(() => {
          this.$refs['dataForm'].clearValidate()
        })
      },
      handleResetTabIndex() {
        if (this.tableData.val0 != undefined) {
          this.tableData.val0 = undefined;
        }
        this.currentTabName = '1';
        this.nextTabName = '1';
      },
      handleRowDbClick(row) {
        let _parameters = row.producerConfig.parameters;
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
            this.dialogFormVisible = false
            this.findProducerNames();
            this.$message.success("添加成功")
          })
        })
      },
      updateData() {
        this.generateParameter();
        this.$refs['dataForm'].validate((valid) => {
          if (!valid) {
            return;
          }
          let tempData = Object.assign({}, this.temp)//copy obj
          tempData.type = this.type;
          commonConfigApi.updateCommonConfig(tempData).then((res) => {
            this.dialogFormVisible = false;
            this.findProducerNames();
            this.$message.success("保存成功");
          })
        });
      },
      deleteData(idx, dbQueryName) {
        this.$confirm('您确定要永久删除该查询语句？', '提示', confirm).then(() => {
          commonConfigApi.deleteCommonConfig(dbQueryName.id).then(res => {
            this.$message.success("删除成功");
            this.producerNamesData.splice(idx, 1);
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
            return arr[i];
          }
        }
      },
      generateParameter() {
        const tempEntity = this.temp;
        const message = tempEntity.producerConfig.message;
        let parameters = getParameters(message);
        let newParameterObj = parameters ? parameters.reduce((obj, currentValue) => {
          obj[currentValue] = {
            name: currentValue,
            label: null,
            defaultValue: null
          };
          return obj;
        }, {}) : {};
        //原参数由数组转成对象
        let oldParameters = tempEntity.producerConfig.parameters;
        let oldParameterObj = oldParameters ? oldParameters.reduce((obj, currentValue) => {
          obj[currentValue.name] = currentValue;
          return obj;
        }, {}) : undefined;
        //重置 并使用新的参数信息
        tempEntity.producerConfig.parameters = [];
        let idx = 0;
        for (let parameterName in newParameterObj) {
          if (oldParameterObj && oldParameterObj[parameterName]) {
            tempEntity.producerConfig.parameters.splice(idx, 0, oldParameterObj[parameterName]);
          } else {
            //新的参数
            tempEntity.producerConfig.parameters.splice(idx, 0, {
              name: parameterName,
              label: parameterName,
              defaultValue: null
            });
          }
          idx++;
        }
      },
      validateParamters(row, idx) {
        //验证参数是否为空
        let _parameters = row.producerConfig.parameters;
        let invalidCount = 0;
        if (_parameters !== null && _parameters.length > 0) {
          for (let i in _parameters) {
            if (_parameters[i].defaultValue === undefined || _parameters[i].defaultValue === null || _parameters[i].defaultValue.trim() === '') {
              this.$message.warning('参数[' + _parameters[i].label + "]不能为空");
              this.$set(this.tabLoading, "loading" + parseInt(this.currentTabName), false);
              if (idx) {
                this.$set(this.tableData, "loading" + idx, false);
              }
              invalidCount++;
            }
          }
        }
        return invalidCount === 0;
      },
      executeData(idx, row) {
        //展开
        this.$refs['sqlListTable'].toggleRowExpansion(row, true);
        //设置当前标签页
        this.currentTabName = this.nextTabName;
        //设置当前标签页的loading
        this.$set(this.tabLoading, "loading" + parseInt(this.currentTabName), true);
        //当前标签页数据置空
        this.$set(this.tableData, "val" + (parseInt(this.currentTabName) - 1), undefined);
        if (!this.validateParamters(row, idx)) {
          return;
        }
        kafkaApi.producer(row).then(res => {
          let responseData = res.data.data;

          //计算下个标签页
          this.nextTabName = parseInt(this.currentTabName) + 1 + "";
          if (parseInt(this.nextTabName) > this.tabDatas.length) {
            this.nextTabName = '1';//循环 超过则从1开始
          }

          this.$set(this.tableData, "val" + (parseInt(this.currentTabName) - 1), responseData);
          this.$set(this.tableData, "loading" + idx, false);
          //设置当前标签页的loading
          this.$set(this.tabLoading, "loading" + parseInt(this.currentTabName), false);
        }).catch(e => {
          console.log(e);
          //设置当前标签页的loading
          this.$set(this.tabLoading, "loading" + parseInt(this.currentTabName), false);
          this.$set(this.tableData, "loading" + idx, false);
          //当前标签页数据置空
          this.$set(this.tableData, "val" + (parseInt(this.currentTabName) - 1), {});
        });
      },
      //查找TOPIC
      findTopic(key) {
        this.topicNamesLoading = true;
        if (key) {
          this.topicNamesQuery.key = key;
        } else {
          this.topicNamesQuery.key = '';
        }
        commonConfigApi.queryCommonConfig(this.topicNamesQuery, this.topicNamesPage).then(res => {
          this.topicNames = res.data.page.records
          this.topicNamesLoading = false
        })
      }
    }
  }
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
</style>
<style rel="stylesheet/scss" lang="scss">
</style>
