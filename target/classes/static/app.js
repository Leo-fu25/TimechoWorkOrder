const state={users:[],departments:[],statuses:[],priorities:[],types:[],workOrders:[],slaPolicies:[],assignmentRules:[],notifications:[],activeTab:"dashboard-tab"};

document.addEventListener("DOMContentLoaded",async()=>{bind();initTabs();await boot();});

function bind(){
  on("refreshButton","click",boot);
  on("exportCsvButton","click",exportCsv);
  on("createForm","submit",createWorkOrder);
  on("filterForm","submit",searchWorkOrders);
  on("resetFilterButton","click",()=>{document.getElementById("filterForm").reset();renderTable(state.workOrders);});
  on("slaForm","submit",createSlaPolicy);
  on("ruleForm","submit",createAssignmentRule);
  on("notificationForm","submit",loadNotifications);
  on("markAllReadButton","click",markAllNotificationsRead);
  on("portalSearchForm","submit",searchPortalWorkOrders);
  on("quickActionForm","submit",quickStatusChange);
  on("assignForm","submit",assignWorkOrder);
  on("batchStatusForm","submit",batchStatusUpdate);
  on("detailForm","submit",loadDetail);
  on("commentForm","submit",submitComment);
  on("attachmentForm","submit",submitAttachment);
  on("evaluationForm","submit",submitEvaluation);
}

function initTabs(){
  const buttons=document.querySelectorAll(".nav-tab");
  buttons.forEach(button=>{
    button.addEventListener("click",()=>switchTab(button.dataset.tab));
  });
  switchTab(state.activeTab);
}

function switchTab(tabId){
  state.activeTab=tabId;
  document.querySelectorAll(".workspace-tab").forEach(tab=>tab.classList.toggle("active",tab.id===tabId));
  document.querySelectorAll(".nav-tab").forEach(tab=>tab.classList.toggle("active",tab.dataset.tab===tabId));
}

async function boot(){
  setStatus("正在同步数据...");
  try{
    const [users,departments,statuses,priorities,types,stats,workOrders,slaPolicies,assignmentRules]=await Promise.all([
      api("/api/users"),api("/api/departments"),api("/api/statuses"),api("/api/priorities"),api("/api/workorder-types"),
      api("/api/workorders/statistics"),api("/api/workorders"),api("/api/sla-policies"),api("/api/assignment-rules")
    ]);
    Object.assign(state,{users,departments,statuses,priorities,types,workOrders,slaPolicies,assignmentRules});
    fillOptions();
    renderStats(stats);
    renderTable(workOrders);
    renderSlaPolicies(slaPolicies);
    renderAssignmentRules(assignmentRules);
    setStatus(`系统在线，已加载 ${workOrders.length} 条工单`);
  }catch(error){
    console.error(error);
    setStatus("系统连接失败");
    toast(error.message||"加载失败");
  }
}

async function createWorkOrder(event){
  event.preventDefault();
  const form=event.currentTarget;
  const data=formData(form);
  data.operatorId=Number(data.requesterId);
  ["requesterId","departmentId"].forEach(k=>data[k]=Number(data[k]));
  ["assigneeId","statusId","priorityId","typeId"].forEach(k=>data[k]=data[k]?Number(data[k]):null);
  ["source","customerName","customerEmail","customerType","productName","impactScope","tags","responseDueAt","dueAt"].forEach(k=>data[k]=data[k]||null);
  await api("/api/workorders",{method:"POST",body:JSON.stringify(data)});
  form.reset();
  toast("工单创建成功");
  await boot();
}

async function searchWorkOrders(event){
  event.preventDefault();
  const params=new URLSearchParams();
  const data=formData(event.currentTarget);
  Object.entries(data).forEach(([k,v])=>{
    if(k==="overdue"){if(v)params.append(k,"true");return;}
    if(v!=="")params.append(k,v);
  });
  const workOrders=await api(`/api/workorders?${params.toString()}`);
  renderTable(workOrders);
  setStatus(`查询完成，返回 ${workOrders.length} 条工单`);
}

async function quickStatusChange(event){
  event.preventDefault();
  const data=formData(event.currentTarget);
  const id=data.workOrderId;
  const body={operatorId:Number(data.operatorId),statusId:Number(data.statusId),remark:data.remark||null};
  await api(`/api/workorders/${id}/status`,{method:"PATCH",body:JSON.stringify(body)});
  toast("状态更新成功");
  await boot();
  if(id)await showDetail(id);
}

async function assignWorkOrder(event){
  event.preventDefault();
  const data=formData(event.currentTarget);
  const id=data.workOrderId;
  const body={operatorId:Number(data.operatorId),assigneeId:Number(data.assigneeId),remark:data.remark||null};
  await api(`/api/workorders/${id}/assignee`,{method:"PATCH",body:JSON.stringify(body)});
  toast("工单指派成功");
  await boot();
  if(id)await showDetail(id);
}

async function batchStatusUpdate(event){
  event.preventDefault();
  const data=formData(event.currentTarget);
  const ids=data.workOrderIds.split(",").map(x=>Number(x.trim())).filter(Boolean);
  const body={operatorId:Number(data.operatorId),workOrderIds:ids,statusId:Number(data.statusId),remark:data.remark||null};
  await api("/api/workorders/batch/status",{method:"POST",body:JSON.stringify(body)});
  toast(`已批量更新 ${ids.length} 条工单`);
  await boot();
}

async function exportCsv(){
  const response=await fetch("/api/workorders/export/csv");
  if(!response.ok){toast("导出失败");return;}
  const text=await response.text();
  const blob=new Blob([text],{type:"text/csv;charset=utf-8;"});
  const url=URL.createObjectURL(blob);
  const link=document.createElement("a");
  link.href=url;
  link.download=`workorders-${new Date().toISOString().slice(0,10)}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

async function createSlaPolicy(event){
  event.preventDefault();
  const form=event.currentTarget;
  const data=formData(form);
  const body={
    name:data.name,
    department:data.departmentId?{id:Number(data.departmentId)}:null,
    type:data.typeId?{id:Number(data.typeId)}:null,
    priority:data.priorityId?{id:Number(data.priorityId)}:null,
    responseHours:Number(data.responseHours),
    resolveHours:Number(data.resolveHours),
    autoEscalate:!!data.autoEscalate,
    escalationPriority:data.escalationPriorityId?{id:Number(data.escalationPriorityId)}:null,
    active:true
  };
  await api("/api/sla-policies",{method:"POST",body:JSON.stringify(body)});
  form.reset();
  toast("SLA策略已创建");
  await boot();
}

async function createAssignmentRule(event){
  event.preventDefault();
  const form=event.currentTarget;
  const data=formData(form);
  const body={
    name:data.name,
    priority:Number(data.priority||100),
    source:data.source||null,
    customerType:data.customerType||null,
    department:data.departmentId?{id:Number(data.departmentId)}:null,
    type:data.typeId?{id:Number(data.typeId)}:null,
    targetAssignee:{id:Number(data.targetAssigneeId)},
    active:true
  };
  await api("/api/assignment-rules",{method:"POST",body:JSON.stringify(body)});
  form.reset();
  toast("分派规则已创建");
  await boot();
}

async function loadNotifications(event){
  event.preventDefault();
  const data=formData(event.currentTarget);
  const userId=Number(data.userId);
  const [notifications,unread]=await Promise.all([
    api(`/api/notifications?userId=${userId}`),
    api(`/api/notifications/unread-count?userId=${userId}`)
  ]);
  state.notifications=notifications;
  renderNotifications(notifications,unread.unreadCount||0,userId);
}

async function markAllNotificationsRead(){
  const userId=Number(document.getElementById("notificationUserId").value||0);
  if(!userId){toast("先选择通知接收人");return;}
  await api(`/api/notifications/read-all?userId=${userId}`,{method:"PATCH"});
  await loadNotifications({preventDefault:()=>{},currentTarget:document.getElementById("notificationForm")});
  toast("已全部标记已读");
}

async function markNotificationRead(id,userId){
  await api(`/api/notifications/${id}/read`,{method:"PATCH"});
  const [notifications,unread]=await Promise.all([
    api(`/api/notifications?userId=${userId}`),
    api(`/api/notifications/unread-count?userId=${userId}`)
  ]);
  state.notifications=notifications;
  renderNotifications(notifications,unread.unreadCount||0,userId);
}

async function searchPortalWorkOrders(event){
  event.preventDefault();
  switchTab("customer-tab");
  const data=formData(event.currentTarget);
  const params=new URLSearchParams();
  params.append("customerEmail",data.customerEmail);
  if(data.keyword){params.append("keyword",data.keyword);}
  if(data.statusId){params.append("statusId",data.statusId);}
  const workOrders=await api(`/api/portal/workorders?${params.toString()}`);
  renderPortalWorkOrders(workOrders,data.customerEmail);
}

async function loadDetail(event){
  event.preventDefault();
  const id=document.getElementById("detailWorkOrderId").value;
  if(!id){toast("请先输入工单 ID");return;}
  await showDetail(id);
}

async function submitEvaluation(event){
  event.preventDefault();
  const form=event.currentTarget;
  const data=formData(form);
  const id=data.workOrderId;
  delete data.workOrderId;
  ["evaluatorId","requirementValue","developmentEffort","customerWeight","competitorImpact","impactScopeScore"].forEach(k=>data[k]=Number(data[k]));
  await api(`/api/workorders/${id}/evaluations`,{method:"POST",body:JSON.stringify(data)});
  form.reset();
  toast("需求评估已提交");
  await boot();
  if(id)await showDetail(id);
}

async function submitComment(event){
  event.preventDefault();
  const form=event.currentTarget;
  const data=formData(form);
  const id=data.workOrderId;
  const body={userId:Number(data.userId),content:data.content,internalOnly:!!data.internalOnly};
  await api(`/api/workorders/${id}/comments`,{method:"POST",body:JSON.stringify(body)});
  form.querySelector('textarea[name="content"]').value="";
  form.querySelector('input[name="internalOnly"]').checked=false;
  toast("评论已提交");
  await boot();
  if(id)await showDetail(id);
}

async function submitAttachment(event){
  event.preventDefault();
  const form=event.currentTarget;
  const data=formData(form);
  const id=data.workOrderId;
  const body={
    uploadedById:Number(data.uploadedById),
    fileName:data.fileName,
    fileUrl:data.fileUrl,
    contentType:data.contentType||null,
    sizeBytes:data.sizeBytes?Number(data.sizeBytes):null
  };
  await api(`/api/workorders/${id}/attachments`,{method:"POST",body:JSON.stringify(body)});
  form.reset();
  toast("附件元数据已登记");
  await boot();
  if(id)await showDetail(id);
}

async function showDetail(id){
  switchTab("collaboration-tab");
  const [workOrder,history,evaluations,comments,attachments]=await Promise.all([
    api(`/api/workorders/${id}`),
    api(`/api/workorders/${id}/history`),
    api(`/api/workorders/${id}/evaluations`),
    api(`/api/workorders/${id}/comments`),
    api(`/api/workorders/${id}/attachments`)
  ]);
  syncDetailForms(id);
  const box=document.getElementById("workOrderDetail");
  box.classList.remove("empty");
  box.innerHTML=`
    <div class="detail-section">
      <h3>#${workOrder.id} ${esc(workOrder.title)}</h3>
      <div class="detail-meta">${esc(workOrder.type?.description||"未分类")} / ${esc(workOrder.priority?.description||"未设置优先级")} / ${esc(workOrder.status?.description||"未设置状态")}</div>
      <div>${esc(workOrder.description||"-")}</div>
    </div>
    <div class="detail-section">
      <h3>流转历史</h3>
      <div class="timeline">${cards(history,item=>`${fmt(item.createdAt)} · ${esc(item.user?.name||"-")} · ${esc(item.action||"-")}`,item=>esc(item.description||"-"),"暂无历史记录")}</div>
    </div>
    <div class="detail-section">
      <h3>评论</h3>
      <div class="timeline">${cards(comments,item=>`${fmt(item.createdAt)} · ${esc(item.user?.name||"-")} · ${item.internalOnly?"内部":"公开"}`,item=>esc(item.content||"-"),"暂无评论")}</div>
    </div>
    <div class="detail-section">
      <h3>附件</h3>
      <div class="timeline">${cards(attachments,item=>`${fmt(item.createdAt)} · ${esc(item.uploadedBy?.name||"-")}`,item=>`<a href="${esc(item.fileUrl||"#")}" target="_blank" rel="noopener noreferrer">${esc(item.fileName||"-")}</a><br>${esc(item.contentType||"未标注类型")} / ${esc(String(item.sizeBytes??"-"))}`, "暂无附件")}</div>
    </div>
    <div class="detail-section">
      <h3>需求评估</h3>
      <div class="timeline">${cards(evaluations,item=>`${fmt(item.createdAt)} · ${esc(item.evaluator?.name||"-")}`,item=>`总分：${esc(String(item.totalScore))}<br>${esc(item.comment||"无备注")}`,"暂无评估记录")}</div>
    </div>`;
}

function syncDetailForms(id){
  document.getElementById("detailWorkOrderId").value=id;
  document.getElementById("commentWorkOrderId").value=id;
  document.getElementById("attachmentWorkOrderId").value=id;
}

function fillOptions(){
  fill("requesterId",state.users,u=>`${u.name} (${u.username})`);
  fill("assigneeId",state.users,u=>`${u.name} (${u.username})`,true);
  fill("departmentId",state.departments,x=>x.name);
  fill("statusId",state.statuses,x=>x.description);
  fill("priorityId",state.priorities,x=>x.description);
  fill("typeId",state.types,x=>x.description);
  fill("filterStatusId",state.statuses,x=>x.description,true);
  fill("filterPriorityId",state.priorities,x=>x.description,true);
  fill("filterTypeId",state.types,x=>x.description,true);
  fill("filterAssigneeId",state.users,u=>`${u.name} (${u.username})`,true);
  fill("quickStatusId",state.statuses,x=>x.description);
  fill("quickOperatorId",state.users,u=>`${u.name} (${u.username})`);
  fill("assignAssigneeId",state.users,u=>`${u.name} (${u.username})`);
  fill("assignOperatorId",state.users,u=>`${u.name} (${u.username})`);
  fill("batchStatusId",state.statuses,x=>x.description);
  fill("batchOperatorId",state.users,u=>`${u.name} (${u.username})`);
  fill("commentUserId",state.users,u=>`${u.name} (${u.username})`);
  fill("attachmentUserId",state.users,u=>`${u.name} (${u.username})`);
  fill("evaluatorId",state.users,u=>`${u.name} (${u.username})`);
  fill("slaPriorityId",state.priorities,x=>x.description,true);
  fill("slaEscalationPriorityId",state.priorities,x=>x.description,true);
  fill("slaTypeId",state.types,x=>x.description,true);
  fill("slaDepartmentId",state.departments,x=>x.name,true);
  fill("ruleDepartmentId",state.departments,x=>x.name,true);
  fill("ruleTypeId",state.types,x=>x.description,true);
  fill("ruleAssigneeId",state.users,u=>`${u.name} (${u.username})`);
  fill("notificationUserId",state.users,u=>`${u.name} (${u.username})`);
  fill("portalStatusId",state.statuses,x=>x.description,true);
}

function renderStats(stats){
  const items=[
    ["总工单",stats.total??0],["待处理",stats.pending??0],["处理中",stats.inProgress??0],["已完成",stats.completed??0],
    ["超时工单",stats.overdue??0],["响应超时",stats.responseTimeout??0],["平均处理时长",`${Number(stats.averageHandleHours||0).toFixed(1)} h`],["类型数",Object.keys(stats.typeDistribution||{}).length]
  ];
  document.getElementById("heroTotal").textContent=stats.total??0;
  document.getElementById("heroProgress").textContent=stats.inProgress??0;
  document.getElementById("heroOverdue").textContent=stats.overdue??0;
  document.getElementById("metricGrid").innerHTML=items.map(([l,v])=>`<div class="metric-card"><span class="eyebrow">${esc(l)}</span><strong>${esc(String(v))}</strong></div>`).join("");
}

function renderTable(rows){
  const body=document.getElementById("workOrderTableBody");
  document.getElementById("tableCount").textContent=`${rows.length} 条记录`;
  body.innerHTML=rows.length?rows.map(x=>`
    <tr>
      <td>${x.id}</td>
      <td>${esc(x.title||"-")}</td>
      <td><span class="tag type">${esc(x.type?.description||"-")}</span></td>
      <td><span class="tag priority">${esc(x.priority?.description||"-")}</span></td>
      <td><span class="tag status">${esc(x.status?.description||"-")}</span></td>
      <td>${esc(x.requester?.name||"-")}</td>
      <td>${esc(x.assignee?.name||"未指派")}</td>
      <td>${esc(x.productName||"-")}</td>
      <td>${fmt(x.dueAt)}</td>
      <td><button class="ghost" onclick="openWorkOrderDetailFromList(${x.id})">详情</button></td>
    </tr>`).join(""):`<tr><td colspan="10" class="empty">当前没有符合条件的工单</td></tr>`;
}

async function openWorkOrderDetailFromList(id){
  await showDetail(id);
}

function renderSlaPolicies(policies){
  const container=document.getElementById("slaList");
  container.innerHTML=policies.length?policies.map(policy=>`
    <div class="list-item">
      <div class="detail-meta">${esc(policy.name)} · ${policy.active?"启用":"停用"}</div>
      <div>匹配：${esc(policy.department?.name||"全部部门")} / ${esc(policy.type?.description||"全部类型")} / ${esc(policy.priority?.description||"全部优先级")}</div>
      <div>时限：响应 ${esc(String(policy.responseHours))}h，解决 ${esc(String(policy.resolveHours))}h，自动升级 ${policy.autoEscalate?"是":"否"}</div>
    </div>
  `).join(""):`<div class="list-item">暂无SLA策略</div>`;
}

function renderAssignmentRules(rules){
  const container=document.getElementById("ruleList");
  container.innerHTML=rules.length?rules.map(rule=>`
    <div class="list-item">
      <div class="detail-meta">${esc(rule.name)} · 顺序 ${esc(String(rule.priority))} · ${rule.active?"启用":"停用"}</div>
      <div>匹配：${esc(rule.department?.name||"全部部门")} / ${esc(rule.type?.description||"全部类型")} / ${esc(rule.source||"全部来源")} / ${esc(rule.customerType||"全部客户类型")}</div>
      <div>分派给：${esc(rule.targetAssignee?.name||"-")}</div>
    </div>
  `).join(""):`<div class="list-item">暂无分派规则</div>`;
}

function renderNotifications(notifications,unreadCount,userId){
  document.getElementById("notificationUnread").textContent=`未读：${unreadCount}`;
  const container=document.getElementById("notificationList");
  container.innerHTML=notifications.length?notifications.map(item=>`
    <div class="list-item">
      <div class="detail-meta">${fmt(item.createdAt)} · ${esc(item.eventType||"-")} · ${item.readFlag?"已读":"未读"}</div>
      <div>${esc(item.message||"-")}</div>
      ${item.readFlag?"":`<div class="buttons"><button class="ghost" onclick="markNotificationRead(${item.id},${userId})">标记已读</button></div>`}
    </div>
  `).join(""):`<div class="list-item">暂无通知</div>`;
}

function renderPortalWorkOrders(workOrders,customerEmail){
  const container=document.getElementById("portalWorkOrderList");
  container.innerHTML=workOrders.length?workOrders.map(item=>`
    <div class="list-item">
      <div class="detail-meta">#${item.id} · ${esc(item.status?.description||"-")} · ${fmt(item.createdAt)}</div>
      <div>${esc(item.title||"-")}</div>
      <div class="buttons">
        <button class="ghost" onclick="openPortalComments(${item.id},'${esc(customerEmail)}')">查看客户可见评论</button>
      </div>
    </div>
  `).join(""):`<div class="list-item">当前客户暂无工单</div>`;
}

async function openPortalComments(workOrderId,customerEmail){
  switchTab("customer-tab");
  const comments=await api(`/api/portal/workorders/${workOrderId}/comments?customerEmail=${encodeURIComponent(customerEmail)}`);
  const panel=document.getElementById("portalCommentPreview");
  if(!comments.length){
    panel.classList.add("empty");
    panel.textContent="该工单暂无客户可见评论。";
    return;
  }
  panel.classList.remove("empty");
  panel.innerHTML=comments.map(item=>`
    <div class="detail-section">
      <div class="detail-meta">${fmt(item.createdAt)} · ${esc(item.user?.name||"-")}</div>
      <div>${esc(item.content||"-")}</div>
    </div>
  `).join("");
}

function fill(id,items,label,keepFirst){
  const el=document.getElementById(id); if(!el)return;
  const prev=el.value; const first=keepFirst?el.options[0]?.outerHTML||'<option value="">请选择</option>':"";
  el.innerHTML=first;
  items.forEach(item=>{const op=document.createElement("option");op.value=item.id;op.textContent=label(item);el.appendChild(op);});
  if(prev&&[...el.options].some(x=>x.value===prev))el.value=prev;
}

function cards(items,meta,body,empty){return items.length?items.map(item=>`<div class="card"><div class="detail-meta">${meta(item)}</div><div>${body(item)}</div></div>`).join(""):`<div class="card">${empty}</div>`;}
function on(id,event,handler){document.getElementById(id).addEventListener(event,handler);}
function setStatus(msg){document.getElementById("systemStatus").textContent=msg;}
function fmt(v){return v?v.replace("T"," ").slice(0,16):"-";}
function formData(form){const r={};new FormData(form).forEach((v,k)=>r[k]=typeof v==="string"?v.trim():v);form.querySelectorAll('input[type="checkbox"]').forEach(i=>r[i.name]=i.checked);return r;}
function toast(msg){const el=document.getElementById("toast");el.textContent=msg;el.classList.remove("hidden");clearTimeout(toast.t);toast.t=setTimeout(()=>el.classList.add("hidden"),2800);}
function esc(v){return String(v).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;").replaceAll("'","&#39;");}

async function api(url,options={}){
  const response=await fetch(url,{headers:{"Content-Type":"application/json"},...options});
  if(!response.ok){
    let message=`请求失败: ${response.status}`;
    try{const error=await response.json();message=error.message||message;}catch(e){}
    throw new Error(message);
  }
  if(response.status===204)return null;
  return response.json();
}
