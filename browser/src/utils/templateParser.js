/**
 * 从一段字符串中获取参数列表
 * @param str
 * @returns {Array}
 */
export function getParameters(str) {
  let g = /\$\{(.*?)\}/g;//匹配${}
  let funReg = /^.*\(.*\)$/;//函数匹配
  let group = str.match(g);
  if (group) {
    let arr = [];
    for (let idx in group) {
      const item = group[idx];
      const val = item.substring(2, item.length - 1).trim();
      if (!val.match(funReg)) {
        //不是函数才加进参数列表，涵数不是参数，由后台模板渲染
        arr.splice(idx, 1, val);
      }
    }
    return arr;
  }
  return [];
}
