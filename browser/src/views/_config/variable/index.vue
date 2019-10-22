<template>
  <div class="app-container">
    <!--查询  -->
    <el-row>
      <el-input style="width:200px;" v-model="tableQuery.name" placeholder="变量名"></el-input>
      <el-input style="width:200px;" v-model="tableQuery.value" placeholder="变量值"></el-input>
      <el-input style="width:200px;" v-model="tableQuery.desc" placeholder="描述"></el-input>
      <span style="margin-right: 15px;"></span>
      <el-tooltip class="item" content="搜索" placement="top">
        <el-button icon="el-icon-search" circle @click="fetchData(1)"></el-button>
      </el-tooltip>
    </el-row>
    <div style="margin-bottom: 30px;"></div>
    <el-button type="primary" icon="el-icon-plus" size="mini" @click="handleCreate">{{textMap.create}}</el-button>
    <div style="margin-bottom: 30px;"></div>
    <!--列表-->
    <el-table style="width: 100%"
              :data="tableData"
              v-loading.body="tableLoading"
              element-loading-text="加载中"
              border fit highlight-current-row>
      <el-table-column prop="name" label="变量名"></el-table-column>
      <el-table-column prop="value" label="变量值"></el-table-column>
      <el-table-column prop="desc" label="描述"></el-table-column>
      <el-table-column prop="time" label="创建时间">
        <template slot-scope="scope">
          <span v-text="parseTime(scope.row.created)"></span>
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
            <el-button @click="handleUpdate(scope.$index,scope.row)" size="medium" type="info" icon="el-icon-edit" :disabled="scope.row.creator === undefined || scope.row.creator === null || (currentUser !== 'admin' && scope.row.creator !== currentUser)"
                       circle plain></el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button @click="handleDelete(scope.$index,scope.row)" size="medium" type="danger" icon="el-icon-delete" :disabled="scope.row.creator === undefined || scope.row.creator === null || (currentUser !== 'admin' && scope.row.creator !== currentUser)"
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
    <!--弹出窗口：新增/编辑变量-->
    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="80%">
      <el-form :rules="rules" ref="dataForm" :model="temp" label-position="left" label-width="120px">

        <el-form-item label="变量名" prop="name" v-if="dialogStatus=='create'">
          <el-input v-model="temp.name"></el-input>
        </el-form-item>

        <el-form-item label="变量值" prop="value">
          <el-input v-model="temp.value"></el-input>
        </el-form-item>

        <el-form-item label="描述" prop="desc">
          <el-input v-model="temp.desc"></el-input>
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

  import { mapState } from 'vuex'
  import variableApi from '@/api/config/variable'
  import {parseTime, resetTemp} from '@/utils'
  import {confirm, pageParamNames, root} from '@/utils/constants'
  import debounce from 'lodash/debounce'

  export default {

    name: 'VariableManage',

    data() {

      let validateNotNull = (rule, value, callback) => {
        if (this.dialogStatus == 'create' && value === '') {
          callback(new Error('必填'));
        } else {
          callback();
        }
      };

      return {
        parseTime: parseTime,
        tableLoading: false,
        tableData: [],
        tableQuery: {
          name: null,
          value: null,
          desc: null
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
          name: null,
          value: null,
          desc: null,
          created: null,
          updated: null
        },
        textMap: {
          update: '编辑变量',
          create: '新增变量'
        },
        rules: {
          name: [{validator: validateNotNull, trigger: 'blur'}],
          value: [{validator: validateNotNull, trigger: 'blur'}]
        },
        checkAll: false,
        isIndeterminate: true,
        //所有角色(管理员除外)
        roleOptions: [],
        roleMap: new Map(),
        // 更新用户的角色的数据
        updateUserRolesData: {
          idx: null,
          uid: null,
          rids: []
        },
      }

    },

    created() {
      this.fetchData()
    },

    watch: {
      //延时查询
      'tableQuery.nick': debounce(function () {
        this.fetchData()
      }, 500)
    },//watch
    computed: {
      ...mapState({
        currentUser: function (state) {
          return state.user.name;
        }
      })
    },
    methods: {
      hasAdminRole(row) {
        if (row && row.roleList) {
          return row.roleList.some(role => role.rval == root.rval)
        }
        return false
      },

      //全选
      handleCheckAllChange(val) {
        let allRids = this.roleOptions.map(role => role.id)
        this.updateUserRolesData.rids = val ? allRids : [];
        this.isIndeterminate = false;
      },

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
        this.tableLoading = true
        variableApi.queryVariable(this.tableQuery, this.tablePage).then(res => {
          this.tableData = res.data.page.records
          this.tableLoading = false
          pageParamNames.forEach(name => this.$set(this.tablePage, name, res.data.page[name]))
        })
      },

      //更新
      handleUpdate(idx, row) {
        this.temp = Object.assign({}, row) // copy obj
        this.temp.idx = idx
        this.dialogStatus = 'update'
        this.dialogFormVisible = true
        this.$nextTick(() => this.$refs['dataForm'].clearValidate())
      },
      updateData() {
        this.$refs['dataForm'].validate((valid) => {
          if (!valid) return
          const tempData = Object.assign({}, this.temp)//copy obj
          variableApi.updateVariable(tempData).then(res => {
            tempData.updated = res.data.data.updated
            this.tableData.splice(tempData.idx, 1, tempData)
            this.dialogFormVisible = false
            this.$message.success("更新成功")
          })
        })
      },

      //删除
      handleDelete(idx, row) {
        this.$message.info("此功能暂未开放");
        // this.$confirm('您确定要永久删除该用户？', '提示', confirm).then(() => {
        //   variableApi.deleteUser({uid: row.uid}).then(res => {
        //     this.tableData.splice(idx, 1)
        //     --this.tablePage.total
        //     this.dialogFormVisible = false
        //     this.$message.success("删除成功")
        //   })
        // }).catch(() => {
        //   this.$message.info("已取消删除")
        // });
      },

      //新增
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
          if (!valid) return;
          variableApi.addVariable(this.temp).then((res) => {
            this.temp = res.data.data;
            this.tableData.unshift(Object.assign({}, this.temp));
            ++this.tablePage.total;
            this.dialogFormVisible = false
            this.$message.success("添加成功")
          })
        })
      },

    }
  }
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
  .role-checkbox {
    margin-left: 0px;
    margin-right: 15px;
  }
</style>
