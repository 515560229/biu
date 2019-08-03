//写一些练习的函数
let a = [{'username': 'zhangsan', age: 30}, {'username': 'lisi', age: 10}];
// 数组转map
let e = a.reduce((obj, currentValue) => {
  obj[currentValue.username] = currentValue;
  return obj;
}, {});
console.log(JSON.stringify(e, null, 2));
console.log("----------");
//map转数组
console.log(JSON.stringify(Object.values(a), null, 2));

