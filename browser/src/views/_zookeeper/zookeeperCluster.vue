<template>
  <div class="app-container">
    <!--查询  -->
    <el-row>
      <el-input style="width:200px;" v-model="tableQuery.key" placeholder="关键字"></el-input>
      <span style="margin-right: 15px;"></span>
      <el-tooltip class="item" content="搜索" placement="top">
        <el-button icon="el-icon-search" circle @click="fetchData(1)"></el-button>
      </el-tooltip>
    </el-row>
    <div style="margin-bottom: 30px;"></div>
    <el-button type="primary" icon="el-icon-plus" size="mini" @click="handleCreate">
      {{textMap.create}}
    </el-button>
    <div style="margin-bottom: 30px;"></div>
    <!--列表-->
    <el-table style="width: 100%"
              :data="tableData"
              v-loading.body="tableLoading"
              element-loading-text="加载中"
              border fit highlight-current-row>
      <el-table-column prop="name" label="集群名称"></el-table-column>
      <el-table-column prop="url" label="url">
        <template slot-scope="scope">
          <span v-text="scope.row.zkClusterConfig.url"></span>
        </template>
      </el-table-column>
      <el-table-column prop="time" label="更新时间">
        <template slot-scope="scope">
          <span v-text="parseTime(scope.row.updated)"></span>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template slot-scope="scope">
          <el-tooltip content="编辑" placement="top">
            <el-button @click="handleUpdate(scope.$index,scope.row)" size="medium" type="info"
                       icon="el-icon-edit"
                       :disabled="scope.row.creator === undefined || scope.row.creator === null || (currentUser !== 'admin' && scope.row.creator !== currentUser)"
                       circle plain></el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button @click="handleDelete(scope.$index,scope.row)" size="medium" type="danger"
                       icon="el-icon-delete"
                       :disabled="scope.row.creator === undefined || scope.row.creator === null || (currentUser !== 'admin' && scope.row.creator !== currentUser)"
                       circle plain></el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-bottom: 30px;"></div>
    <!--分页-->
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="tablePage.current"
      :page-sizes="[10, 20, 30, 40, 50]"
      :page-size="tablePage.size"
      layout="total, sizes, prev, pager, next, jumper"
      :total="tablePage.total">
    </el-pagination>
    <!--弹出窗口：新增/编辑-->
    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="80%">
      <el-form :rules="rules" ref="dataForm" :model="temp" label-position="left"
               label-width="120px">

        <el-form-item label="集群名称" prop="name" v-if="dialogStatus=='create'">
          <el-input v-model="temp.name"></el-input>
        </el-form-item>

        <el-form-item label="url" prop="url">
          <el-input v-model="temp.zkClusterConfig.url"
                    placeholder="多个用,隔开,如：localhost:2181,localhost:2182"></el-input>
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

  import {mapState} from 'vuex'
  import commonConfigApi from '@/api/config/commonConfig'
  import {parseTime, resetTemp} from '@/utils'
  import {confirm, pageParamNames, root} from '@/utils/constants'
  import debounce from 'lodash/debounce'

  export default {

    name: 'ZookeeperClusterManager',

    data() {

      let validateNotNull = (rule, value, callback) => {
        if (this.dialogStatus == 'create' && value === '') {
          callback(new Error('必填'));
        } else {
          callback();
        }
      };

      return {
        type: "zkClus",
        parseTime: parseTime,
        tableLoading: false,
        tableData: [],
        tableQuery: {
          key: null,
          type: null
        },
        tablePage: {
          current: null,
          pages: null,
          size: null,
          total: null
        },
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
          zkClusterConfig: {
            url: null
          }
        },
        textMap: {
          update: '编辑',
          create: '新增'
        },
        rules: {
          name: [{validator: validateNotNull, trigger: 'blur'}],
          host: [{validator: validateNotNull, trigger: 'blur'}]
        },
        checkAll: false,
        isIndeterminate: true,
      }
    },
    mounted() {
      this.fetchData()
    },
    destroyed() {
    },
    created() {
    },
    computed: {
      ...mapState({
        currentUser: function (state) {
          return state.user.name;
        }
      })
    },
    watch: {
      //延时查询
      'tableQuery.key': debounce(function () {
        this.fetchData()
      }, 300)
    },//watch

    methods: {
      //分页
      handleSizeChange(val) {
        this.tablePage.size = val;
        this.fetchData();
      },
      handleCurrentChange(val) {
        this.tablePage.current = val;
        this.fetchData();
      },
      //查询
      fetchData(current) {
        if (current) {
          this.tablePage.current = current
        }
        this.tableLoading = true;

        this.tableQuery.type = this.type;
        commonConfigApi.queryCommonConfig(this.tableQuery, this.tablePage).then(res => {
          this.tableData = res.data.page.records
          this.tableLoading = false
          pageParamNames.forEach(name => this.$set(this.tablePage, name, res.data.page[name]))
        })
      },

      //更新
      handleUpdate(idx, row) {
        this.temp = Object.assign({}, row) // copy obj
        this.temp.idx = idx;
        this.dialogStatus = 'update';
        this.dialogFormVisible = true;
        this.$nextTick(() => this.$refs['dataForm'].clearValidate())
      },
      updateData() {
        this.$refs['dataForm'].validate((valid) => {
          if (!valid) {
            return
          }
          let tempData = Object.assign({}, this.temp)//copy obj
          tempData.type = this.type;
          commonConfigApi.updateCommonConfig(tempData).then(res => {
            tempData.updated = res.data.data.updated
            this.tableData.splice(tempData.idx, 1, tempData)
            this.dialogFormVisible = false
            this.$message.success("更新成功")
          })
        })
      },
      //删除
      handleDelete(idx, row) {
        this.$confirm('您确定要永久删除该数据？', '提示', confirm).then(() => {
          commonConfigApi.deleteCommonConfig(row.id).then(res => {
            this.tableData.splice(idx, 1)
            --this.tablePage.total
            this.dialogFormVisible = false
            this.$message.success("删除成功")
          })
        }).catch(() => {
          this.$message.info("已取消删除")
        });

      },

      //新增
      handleCreate() {
        resetTemp(this.temp);
        this.dialogStatus = 'create';
        this.dialogFormVisible = true;
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
            this.tableData.unshift(Object.assign({}, this.temp));
            ++this.tablePage.total;
            this.dialogFormVisible = false
            this.$message.success("添加成功")
          })
        })
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
