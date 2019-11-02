import Vue from 'vue'
import Router from 'vue-router'

const _import = require('./_import_' + process.env.NODE_ENV)
// in development-env not use lazy-loading, because lazy-loading too many pages will cause webpack hot update too slow. so only in production use lazy-loading;
// detail: https://panjiachen.github.io/vue-element-admin-site/#/lazy-loading

Vue.use(Router)

/* Layout */
import Layout from '../views/layout/Layout'

/** note: submenu only apppear when children.length>=1
 *   detail see  https://panjiachen.github.io/vue-element-admin-site/#/router-and-nav?id=sidebar
 **/

/**
 * hidden: true                   if `hidden:true` will not show in the sidebar(default is false)
 * alwaysShow: true               if set true, will always show the root menu, whatever its child routes length
 *                                if not set alwaysShow, only more than one route under the children
 *                                it will becomes nested mode, otherwise not show the root menu
 * redirect: noredirect           if `redirect:noredirect` will no redirct in the breadcrumb
 * name:'router-name'             the name is used by <keep-alive> (must set!!!)
 * meta : {
    roles: ['admin','editor']     will control the page roles (you can set multiple roles)
    title: 'title'               the name show in submenu and breadcrumb (recommend set)
    icon: 'svg-name'             the icon show in the sidebar,
    noCache: true                if true ,the page will no be cached(default is false)
  }
 **/
export const constantRouterMap = [
  {path: '/login', component: _import('login/index'), hidden: true},
  {path: '/authredirect', component: _import('login/authredirect'), hidden: true},
  {path: '/404', component: _import('errorPage/404'), hidden: true},
  {path: '/401', component: _import('errorPage/401'), hidden: true},
  {
    path: '',
    component: Layout,
    redirect: 'dashboard',
    children: [{
      path: 'dashboard',
      component: _import('dashboard/index'),
      name: 'dashboard',
      meta: {title: '首页', icon: 'dashboard', noCache: true}
    }]
  },
]

export default new Router({
  // mode: 'history', // require service support
  scrollBehavior: () => ({y: 0}),
  routes: constantRouterMap
})

export const asyncRouterMap = [
  {
    path: '/examples',
    component: Layout,
    hidden: true,
    meta: {title: '练习', icon: 'chart'},
    children: [
      {
        path: 'examples1',
        name: 'examples1',
        hidden: true,
        component: _import('_examples/mysqlEditor'),
        meta: {title: 'MySQLEditor', icon: 'chart', noCache: false}
      },
      {
        path: 'examples2',
        name: 'examples2',
        hidden: true,
        component: _import('_examples/mysqlEditor2'),
        meta: {title: 'MySQLEditor2', icon: 'chart', noCache: false}
      },
      {
        path: 'tabs',
        name: 'tabs',
        hidden: true,
        component: _import('_examples/tabs'),
        meta: {title: 'tabs练习', icon: 'chart', noCache: false}
      }
    ]
  },
  {
    path: '/docs',
    component: Layout,
    meta: {title: '文档说明', icon: 'chart'},
    children: [
      {
        path: 'project',
        name: 'project',
        component: _import('_docs/projectDoc'),
        meta: {title: '项目说明', icon: 'chart', noCache: true}
      },
      {
        path: 'function',
        name: 'function',
        component: _import('_docs/functions'),
        meta: {title: '自定义函数', icon: 'chart', noCache: true},
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    meta: {perm: 'm:sys', title: '系统', icon: 'chart'},
    children: [
      {
        path: 'user_manage',
        name: 'user_manage',
        component: _import('_system/user/index'),
        meta: {perm: 'm:sys:user', title: '用户管理', icon: 'chart', noCache: true}
      },
      {
        path: 'role_manage',
        name: 'role_manage',
        component: _import('_system/role/index'),
        meta: {perm: 'm:sys:role', title: '角色管理', icon: 'chart', noCache: true},
      },
      {
        hidden: true,
        path: 'role_manage/:roleId/assign_perm',
        name: 'role_manage_assign_perm',
        component: _import('_system/role/assign_perm'),
        meta: {hiddenTag: true, title: '角色授权'},
      },
      {
        path: 'perm_manage',
        name: 'perm_manage',
        component: _import('_system/perm/index'),
        meta: {perm: 'm:sys:perm', title: '权限管理', icon: 'chart', noCache: true}

      },
    ]
  },
  {
    path: '/config',
    component: Layout,
    meta: {title: '配置', icon: 'chart'},
    children: [
      {
        path: 'variable',
        name: 'variable',
        component: _import('_config/variable/index'),
        meta: {title: '全局变量', icon: 'chart', noCache: false}
      },
      {
        path: 'variable2',
        name: 'variable2',
        component: _import('_config/db/index'),
        hidden: true,
        meta: {title: '数据源', icon: 'chart', noCache: false}
      }
    ]
  },
  {
    path: '/operator',
    component: Layout,
    hidden: true,
    meta: {title: '操作', icon: 'chart'},
    children: [
      {
        path: 'dbQueryIndex',
        name: 'dbQueryIndex',
        component: _import('_operator/dbQueryIndex'),
        hidden: true,
        meta: {title: '数据库1', icon: 'chart', noCache: false}
      },
      {
        path: 'kafka',
        name: 'kafka',
        component: _import('_operator/dbQueryIndex2'),
        hidden: true,
        meta: {title: '数据库2', icon: 'chart', noCache: false}
      }
    ]
  },
  {
    path: '/mysql',
    component: Layout,
    meta: {title: 'MySQL', icon: 'chart'},
    children: [
      {
        path: 'dataSource',
        name: 'dataSource',
        component: _import('_mysql/dataSource'),
        meta: {title: '数据源', icon: 'chart', noCache: false}
      },
      {
        path: 'sql',
        name: 'sql',
        component: _import('_mysql/sql'),
        meta: {title: 'SQL', icon: 'chart', noCache: false}
      }
    ]
  },
  {
    path: '/interfaces',
    component: Layout,
    meta: {title: '接口调用', icon: 'chart'},
    children: [
      {
        path: 'http',
        name: 'http',
        component: _import('_interfaces/http'),
        hidden: true,
        meta: {title: 'http1', icon: 'chart', noCache: false}
      },
      {
        path: 'http2',
        name: 'http2',
        component: _import('_interfaces/http2'),
        meta: {title: 'http', icon: 'chart', noCache: false}
      },
      {
        path: 'ws',
        name: 'ws',
        component: _import('_interfaces/ws'),
        meta: {title: 'ws', icon: 'chart', noCache: false}
      }
    ]
  },
  {
    path: '/kafka',
    component: Layout,
    meta: {title: 'Kafka', icon: 'chart'},
    children: [
      {
        path: 'kafkaCluster',
        name: 'kafkaCluster',
        component: _import('_kafka/kafkaCluster'),
        meta: {title: '集群管理', icon: 'chart', noCache: false}
      },
      {
        path: 'consumer',
        name: 'consumer',
        component: _import('_kafka/consumer'),
        meta: {title: '读取消息', icon: 'chart', noCache: false}
      },
      {
        path: 'producer',
        name: 'producer',
        component: _import('_kafka/producer'),
        meta: {title: '写入消息', icon: 'chart', noCache: false}
      }
    ]
  },
  {
    path: '/others',
    component: Layout,
    meta: {title: '其它工具', icon: 'chart'},
    children: [
      {
        path: 'jsonFormat',
        name: 'jsonFormat',
        component: _import('_others/jsonFormat'),
        meta: {title: 'JSON格式化', icon: 'chart', noCache: false}
      },
      {
        path: 'xmlFormat',
        name: 'xmlFormat',
        component: _import('_others/xmlFormat'),
        meta: {title: 'XML格式化', icon: 'chart', noCache: false}
      }
    ]
  },
  /*
  {
    path: '/menu1',
    component: Layout,
    children: [{
      path: 'index',
      name: 'menu1',
      component: _import('menu/menu1'),
      meta: {perm: 'm:menu1', title: '菜单1', icon: 'icon'}
    }]
  },


  {
    path: '/menu2',
    component: Layout,
    children: [{
      path: 'index',
      name: 'menu2',
      component: _import('menu/menu2'),
      meta: {perm: 'm:menu2', title: '菜单2', icon: 'icon'}
    }]
  },

  {
    path: '/menu3',
    component: Layout,
    meta: {
      perm: 'm:menu3',
      title: '菜单3',
      icon: 'chart'
    },
    children: [
      {
        path: 'menu3_1',
        component: _import('menu/menu3_1'),
        name: 'menu3_1',
        meta: {perm: 'm:menu3:1', title: '菜单3-1', icon: 'chart', noCache: true}
      },
      {
        path: 'menu3_2',
        component: _import('menu/menu3_2'),
        name: 'menu3_2',
        meta: {perm: 'm:menu3:2', title: '菜单3-2', icon: 'chart', noCache: true}
      },
      {
        path: 'menu3_3',
        component: _import('menu/menu3_3'),
        name: 'menu3_3',
        meta: {perm: 'm:menu3:3', title: '菜单3-3', icon: 'chart', noCache: true}
      }
    ]
  },
  {
    path: '/menu4',
    name: 'menu4',
    component: Layout,
    redirect: '/menu4/menu4_1/a',
    meta: {
      perm: 'm:menu4',
      title: '菜单4',
      icon: 'example'
    },
    children: [
      {
        path: '/menu4/menu4_1',
        name: 'menu4_1',
        component: _import('menu/menu4_1/index'),
        redirect: '/menu4/menu4_1/a',
        meta: {
          perm: 'm:menu4:1',
          title: '菜单4-1',
          icon: 'table'
        },
        children: [
          {
            path: 'a',
            name: 'menu4_1_a',
            component: _import('menu/menu4_1/a'),
            meta: {perm: 'm:menu4:1:a', title: '菜单4-1-a'}
          },
          {
            path: 'b',
            name: 'menu4_1_b',
            component: _import('menu/menu4_1/b'),
            meta: {perm: 'm:menu4:1:b', title: '菜单4-1-b'}
          },
          {
            path: 'c',
            name: 'menu4_1_c',
            component: _import('menu/menu4_1/c'),
            meta: {perm: 'm:menu4:1:c', title: '菜单4-1-c'}
          }
        ]
      },
      {
        path: 'menu4/menu4_2',
        name: 'menu4_2',
        icon: 'tab',
        component: _import('menu/menu4_2/index'),
        meta: {perm: 'm:menu4:2', title: '菜单4-2'}
      }
    ]
  },
  */
  {path: '*', redirect: '/404', hidden: true}
]
