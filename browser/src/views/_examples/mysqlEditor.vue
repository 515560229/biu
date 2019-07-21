<template>
  <div class="app-container">
    <!--
    <template v-for="item in items">
      <mysql-editor v-model="myValue"></mysql-editor>
      <textarea v-model="myValue"></textarea>
      <br/>
    </template>
    -->
    <el-table
      :data="tableData"
      style="width: 100%">
      <el-table-column type="expand">
        <template slot-scope="props">
          <mysql-editor v-model="props.row.name"></mysql-editor>
        </template>
      </el-table-column>
      <el-table-column
        label="商品 ID"
        prop="name">
      </el-table-column>
      <el-table-column
        align="right">
        <template slot="header" slot-scope="scope">
          <el-input
            v-model="search"
            size="mini"
            placeholder="输入关键字搜索"/>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>

  import {parseTime, resetTemp, isJsonString} from '@/utils'
  import {confirm, pageParamNames, root} from '@/utils/constants'
  import MysqlEditor from "../../components/MysqlEditor/index";

  export default {
    name: 'MysqlEditorExample',
    components: {MysqlEditor},
    data() {
      return {
        search: '',
        myValue: "select * from a",
        items: [
          "a", "b"
        ],
        tableData: [
          {name: "1"},
          {name: "2"},
        ]
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
    watch: {},//watch
    methods: {}
  }
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
  .role-checkbox {
    margin-left: 0px;
    margin-right: 15px;
  }
</style>
