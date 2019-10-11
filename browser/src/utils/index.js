import jsonlint from "jsonlint";//https://github.com/vkiryukhin/pretty-data

/**
 * Created by jiachenpan on 16/11/18.
 */
export function parseTime(time, cFormat) {
  if (arguments.length === 0) {
    return null
  }
  if (!time) return null;
  const format = cFormat || '{y}-{m}-{d} {h}:{i}:{s}'
  let date
  if (typeof time === 'object') {
    date = time
  } else {
    if (('' + time).length === 10) time = parseInt(time) * 1000
    date = new Date(time)
  }
  const formatObj = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  }
  const time_str = format.replace(/{(y|m|d|h|i|s|a)+}/g, (result, key) => {
    let value = formatObj[key]
    if (key === 'a') return ['一', '二', '三', '四', '五', '六', '日'][value - 1]
    if (result.length > 0 && value < 10) {
      value = '0' + value
    }
    return value || 0
  })
  return time_str
}

export function formatTime(time, option) {
  time = +time * 1000
  const d = new Date(time)
  const now = Date.now()

  const diff = (now - d) / 1000

  if (diff < 30) {
    return '刚刚'
  } else if (diff < 3600) { // less 1 hour
    return Math.ceil(diff / 60) + '分钟前'
  } else if (diff < 3600 * 24) {
    return Math.ceil(diff / 3600) + '小时前'
  } else if (diff < 3600 * 24 * 2) {
    return '1天前'
  }
  if (option) {
    return parseTime(time, option)
  } else {
    return d.getMonth() + 1 + '月' + d.getDate() + '日' + d.getHours() + '时' + d.getMinutes() + '分'
  }
}

// 格式化时间
export function getQueryObject(url) {
  url = url == null ? window.location.href : url
  const search = url.substring(url.lastIndexOf('?') + 1)
  const obj = {}
  const reg = /([^?&=]+)=([^?&=]*)/g
  search.replace(reg, (rs, $1, $2) => {
    const name = decodeURIComponent($1)
    let val = decodeURIComponent($2)
    val = String(val)
    obj[name] = val
    return rs
  })
  return obj
}

/**
 *get getByteLen
 * @param {Sting} val input value
 * @returns {number} output value
 */
export function getByteLen(val) {
  let len = 0
  for (let i = 0; i < val.length; i++) {
    if (val[i].match(/[^\x00-\xff]/ig) != null) {
      len += 1
    } else {
      len += 0.5
    }
  }
  return Math.floor(len)
}

export function cleanArray(actual) {
  const newArray = []
  for (let i = 0; i < actual.length; i++) {
    if (actual[i]) {
      newArray.push(actual[i])
    }
  }
  return newArray
}

export function param(json) {
  if (!json) return ''
  return cleanArray(Object.keys(json).map(key => {
    if (json[key] === undefined) return ''
    return encodeURIComponent(key) + '=' +
      encodeURIComponent(json[key])
  })).join('&')
}

export function param2Obj(url) {
  const search = url.split('?')[1]
  if (!search) {
    return {}
  }
  return JSON.parse('{"' + decodeURIComponent(search).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g, '":"') + '"}')
}

export function html2Text(val) {
  const div = document.createElement('div')
  div.innerHTML = val
  return div.textContent || div.innerText
}

export function objectMerge(target, source) {
  /* Merges two  objects,
     giving the last one precedence */

  if (typeof target !== 'object') {
    target = {}
  }
  if (Array.isArray(source)) {
    return source.slice()
  }
  Object.keys(source).forEach((property) => {
    const sourceProperty = source[property]
    if (typeof sourceProperty === 'object') {
      target[property] = objectMerge(target[property], sourceProperty)
    } else {
      target[property] = sourceProperty
    }
  })
  return target
}

export function scrollTo(element, to, duration) {
  if (duration <= 0) return
  const difference = to - element.scrollTop
  const perTick = difference / duration * 10
  setTimeout(() => {
    console.log(new Date())
    element.scrollTop = element.scrollTop + perTick
    if (element.scrollTop === to) return
    scrollTo(element, to, duration - 10)
  }, 10)
}

export function toggleClass(element, className) {
  if (!element || !className) {
    return
  }
  let classString = element.className
  const nameIndex = classString.indexOf(className)
  if (nameIndex === -1) {
    classString += '' + className
  } else {
    classString = classString.substr(0, nameIndex) + classString.substr(nameIndex + className.length)
  }
  element.className = classString
}

export const pickerOptions = [
  {
    text: '今天',
    onClick(picker) {
      const end = new Date()
      const start = new Date(new Date().toDateString())
      end.setTime(start.getTime())
      picker.$emit('pick', [start, end])
    }
  }, {
    text: '最近一周',
    onClick(picker) {
      const end = new Date(new Date().toDateString())
      const start = new Date()
      start.setTime(end.getTime() - 3600 * 1000 * 24 * 7)
      picker.$emit('pick', [start, end])
    }
  }, {
    text: '最近一个月',
    onClick(picker) {
      const end = new Date(new Date().toDateString())
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      picker.$emit('pick', [start, end])
    }
  }, {
    text: '最近三个月',
    onClick(picker) {
      const end = new Date(new Date().toDateString())
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      picker.$emit('pick', [start, end])
    }
  }]

export function getTime(type) {
  if (type === 'start') {
    return new Date().getTime() - 3600 * 1000 * 24 * 90
  } else {
    return new Date(new Date().toDateString())
  }
}

export function debounce(func, wait, immediate) {
  let timeout, args, context, timestamp, result

  const later = function () {
    // 据上一次触发时间间隔
    const last = +new Date() - timestamp

    // 上次被包装函数被调用时间间隔last小于设定时间间隔wait
    if (last < wait && last > 0) {
      timeout = setTimeout(later, wait - last)
    } else {
      timeout = null
      // 如果设定为immediate===true，因为开始边界已经调用过了此处无需调用
      if (!immediate) {
        result = func.apply(context, args)
        if (!timeout) context = args = null
      }
    }
  }

  return function (...args) {
    context = this
    timestamp = +new Date()
    const callNow = immediate && !timeout
    // 如果延时不存在，重新设定延时
    if (!timeout) timeout = setTimeout(later, wait)
    if (callNow) {
      result = func.apply(context, args)
      context = args = null
    }

    return result
  }
}

export function deepClone(source) {
  if (!source && typeof source !== 'object') {
    throw new Error('error arguments', 'shallowClone')
  }
  const targetObj = source.constructor === Array ? [] : {}
  Object.keys(source).forEach((keys) => {
    if (source[keys] && typeof source[keys] === 'object') {
      targetObj[keys] = source[keys].constructor === Array ? [] : {}
      targetObj[keys] = deepClone(source[keys])
    } else {
      targetObj[keys] = source[keys]
    }
  })
  return targetObj
}

/**
 * 清空对象所有属性
 * @param temp
 */
export function resetTemp(temp, ignore) {
  function getDataType(data) {
    var getType = Object.prototype.toString;
    var myType = getType.call(data);//调用call方法判断类型，结果返回形如[object Function]
    return myType.slice(8, -1);//[object Function],即去除了“[object ”的字符串。
  }

  for (let prop in temp) {
    if (ignore != undefined && ignore != null && ignore[prop] != null) {
      continue;
    }
    if (getDataType(temp[prop]) == "Object") {
      resetTemp(temp[prop]);
    } else {
      temp[prop] = null;
    }
  }
  return temp;
}

export function isJsonString(str) {
  try {
    let parse = JSON.parse(str);
    if (typeof parse == "object") {
      return true;
    }
  } catch (e) {
  }
  return false;
}

export function formatString(str) {
  //https://github.com/vkiryukhin/pretty-data
  let pd = require('@/utils/format').pd;
  if (isJsonString(str)) {
    return pd.json(str);
  } else {
    let result = str;
    try {
      result = pd.xml(str);
    } catch (e) {
    }
    return result;
  }
}


let pd = require('pretty-data').pd;

/**
 * JSON格式化字符串
 * @param input 输入字符串
 * @param transfer 是否转义
 * @param returnString 是否返回string. true:则返回字符串，false:则返回对象。
 * @returns {*}
 */
export function jsonFormat(input, transfer, returnString) {
  if (input !== undefined && input.trim() !== '') {
    let tempObj = jsonlint.parse(input);
    if (transfer) {
      renderJson(tempObj);
    }
    if (returnString) {
      return JSON.stringify(tempObj, null, 2);
    }
    return tempObj;
  } else {
    return input;
  }
}

/**
 * 格式化字符串
 * @param input 输入字符串
 * @param transfer 是否转义
 * @param returnString 是否返回string. true:则返回字符串，false:则返回对象。仅对json有效
 * @returns {*}
 */
export function format(input, transfer, returnString) {
  if (input !== undefined && input.trim() !== '') {
    try {
      //先进行json转换
      let tempObj = jsonlint.parse(input);
      if (transfer) {
        renderJson(tempObj);
      }
      if (returnString) {
        return JSON.stringify(tempObj, null, 2);
      }
      return tempObj;
    } catch (e) {
      //再进行xml转换
      try {
        let tempInput = input;
        if (transfer) {
          tempInput = input.replace(new RegExp("&nbsp;", "g"), "")
            .replace(new RegExp("&lt;", "g"), "<")
            .replace(new RegExp("&gt;", "g"), ">")
            .replace(new RegExp("&amp;", "g"), "&")
            .replace(new RegExp("&quot;", "g"), "\"")
            .replace(new RegExp("&apos;", "g"), "'")
            .replace(new RegExp("&times;", "g"), "×")
            .replace(new RegExp("&divde;", "g"), "÷")
        }
        let data = pd.xml(tempInput);
        return data;
      } catch (e) {
        //还报错，则原样返回
        return input;
      }
    }
  } else {
    return input;
  }
}

function renderJsonString(str) {
  try {
    let value = jsonlint.parse(str);
    if (typeof value === "object" || Array.isArray(value)) {
      //如果string转换后是合法的对象,则将该key指定这个对象
      renderJson(value);
    }
    return value;
  } catch (e) {
    return str;//转换json出错，非json串等原样返回
  }
}

function renderJson(obj) {
  for (let key in obj) {
    if (typeof obj[key] === "string") {
      obj[key] = renderJsonString(obj[key]);
    } else if (Array.isArray(obj[key])) {
      for (let idx in obj[key]) {
        if (typeof obj[key][idx] === "string") {
          obj[key][idx] = renderJsonString(obj[key][idx]);
        }
        renderJson(obj[key][idx]);
      }
    }
  }
}
