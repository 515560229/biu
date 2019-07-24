/**
 * 从一段字符串中获取参数列表
 * @param str
 * @returns {Array}
 */
export function getParameters(str) {
  let g = /\$\{(.*?)\}/g;
  let group = str.match(g);
  if (group) {
    let arr = [];
    for (let idx in group) {
      const item = group[idx];
      const val = item.substring(2, item.length - 1).trim();
      arr.splice(idx, 1, val)
    }
    return arr;
  }
  return [];
}
