package com.zoneyet.robot.admin.controller.system.appuser;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.session.Session;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.zoneyet.robot.admin.controller.base.BaseController;
import com.zoneyet.robot.admin.entity.Page;
import com.zoneyet.robot.admin.entity.system.Role;
import com.zoneyet.robot.admin.entity.system.User;
import com.zoneyet.robot.admin.service.system.appuser.AppuserManager;
import com.zoneyet.robot.admin.service.system.fhlog.FHlogManager;
import com.zoneyet.robot.admin.service.system.role.RoleManager;
import com.zoneyet.robot.admin.util.AppUtil;
import com.zoneyet.robot.admin.util.Const;
import com.zoneyet.robot.admin.util.Jurisdiction;
import com.zoneyet.robot.admin.util.MD5;
import com.zoneyet.robot.admin.util.MyDateUtils;
import com.zoneyet.robot.admin.util.ObjectExcelView;
import com.zoneyet.robot.admin.util.PageData;

/** 
 * 类名称：会员管理
 * 创建人：FH Q313596790
 * 修改时间：2014年11月17日
 * @version
 */
@Controller
@RequestMapping(value="/happuser")
public class AppuserController extends BaseController {
	
	String menuUrl = "happuser/listUsers.do"; //菜单地址(权限用)
	@Resource(name="appuserService")
	private AppuserManager appuserService;
	@Resource(name="roleService")
	private RoleManager roleService;
	@Resource(name="fhlogService")
	private FHlogManager FHLOG;
	
	/**显示用户列表
	 * @param page
	 * @return
	 */
	@RequestMapping(value="/listUsers")
	public ModelAndView listUsers(Page page){
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		try{
			pd = this.getPageData();
			String userPhone = pd.getString("userPhone");							//检索条件 关键词userPhone
			if(null != userPhone && !"".equals(userPhone)){
				pd.put("userPhone", userPhone.trim());
			}
			String userNickname = pd.getString("userNickname");							//检索条件 关键词userNickname
			if(null != userNickname && !"".equals(userNickname)){
				pd.put("userNickname", userNickname.trim());
			}
			String createStart = pd.getString("createStart");	//开始时间
			String createEnd = pd.getString("createEnd");		//结束时间
			String today = MyDateUtils.getTodayDateString();
			if(createStart != null && !"".equals(createStart)){
				pd.put("createStart", createStart+" 00:00:00");
			}else{
				pd.put("createStart", MyDateUtils.getDateString(today, -30)+" 00:00:00");
			}
			if(createEnd != null && !"".equals(createEnd)){
				pd.put("createEnd", createEnd+" 23:59:59");
			}else{
				pd.put("createEnd", today+" 23:59:59");
			}
			page.setPd(pd);
			List<PageData>	userList = appuserService.listPdPageUser(page);		//列出会员列表
			pd.put("ROLE_ID", "2");
			List<Role> roleList = roleService.listAllRolesByPId(pd);			//列出会员组角色
			mv.setViewName("system/appuser/appuser_list");
			mv.addObject("userList", userList);
			mv.addObject("roleList", roleList);
			mv.addObject("pd", pd);
			mv.addObject("QX",Jurisdiction.getHC());	//按钮权限
		} catch(Exception e){
			logger.error(e.toString(), e);
		}
		return mv;
	}
	
	/**查看用户
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/view")
	public ModelAndView view() throws Exception{
		if(!Jurisdiction.buttonJurisdiction(menuUrl, "cha")){return null;} //校验权限
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		/*if("admin".equals(pd.getString("USERNAME"))){return null;}	//不能查看admin用户
		pd.put("ROLE_ID", "1");
		List<Role> roleList = roleService.listAllRolesByPId(pd);	//列出所有系统用户角色
*/		List<PageData>	orderList = appuserService.listOrdersByUserNickname(pd);
		mv.setViewName("system/appuser/appuser_view");
		mv.addObject("orderList", orderList);
		mv.addObject("msg", "viewAppU");
		mv.addObject("pd", pd);
		/*mv.addObject("roleList", roleList);*/
		return mv;
	}
	
	/**发送消息
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping(value="/sendNotice")
	public ModelAndView sendNotice() throws Exception{
		PageData pd = new PageData();
		pd = this.getPageData();
		Session session = Jurisdiction.getSession();
		User user = (User)session.getAttribute(Const.SESSION_USER);	/*获取当前登陆用户*/
		String USERNAME = user.getUSERNAME();
		FHLOG.save(USERNAME, "新增给"+pd.getString("userNickName2")+"的私信",super.getIP());
		logBefore(logger, Jurisdiction.getUsername()+"新增消息");
		ModelAndView mv = this.getModelAndView();
		
		
		/*pd.put("USER_ID", this.get32UUID());	//ID
*/		String content = pd.getString("CONTENT");
		//pd.put("title", content);		//私信的标题和内容一样			
		pd.put("managerid", user.getUSER_ID());				//最后登录时间
		pd.put("inuse", "1");
		if(null == appuserService.findByUsername(pd)){
			appuserService.sendNotice(pd);			//判断新增权限
			mv.addObject("msg","success");
		}else{
			mv.addObject("msg","failed");
		}
		mv.setViewName("save_result");
		return mv;
	}
	
	/**修改用户状态
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping(value="/changeStatus")
	@ResponseBody
	public String changeStatus() throws Exception{
		PageData pd = new PageData();
		pd = this.getPageData();
		Session session = Jurisdiction.getSession();
		User user = (User)session.getAttribute(Const.SESSION_USER);	/*获取当前登陆用户*/
		String USERNAME = user.getUSERNAME();
		FHLOG.save(USERNAME, "修改用户"+pd.getString("nickname")+"的status为"+pd.getString("status"),super.getIP());
		logBefore(logger, Jurisdiction.getUsername()+"修改用户状态");
		ResultMsg r=new ResultMsg();
		
		if(null == appuserService.findByUsername(pd)){
			appuserService.changeStatus(pd);			//判断新增权限
			r.setMsg("success");
		}else{
			r.setMsg("failed");
		}
		return JSON.toJSONString(r);
	}
	

	
	/**去新增用户页面
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping(value="/goAddU")
	public ModelAndView goAddU() throws Exception{
		if(!Jurisdiction.buttonJurisdiction(menuUrl, "add")){return null;} //校验权限
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		pd.put("ROLE_ID", "2");
		List<Role> roleList = roleService.listAllRolesByPId(pd);			//列出会员组角色
		mv.setViewName("system/appuser/appuser_edit");
		mv.addObject("msg", "saveU");
		mv.addObject("pd", pd);
		mv.addObject("roleList", roleList);
		return mv;
	}
	
	/**保存用户
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/saveU")
	public ModelAndView saveU() throws Exception{
		
		if(!Jurisdiction.buttonJurisdiction(menuUrl, "add")){return null;} //校验权限
		logBefore(logger, Jurisdiction.getUsername()+"新增会员");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		pd.put("USER_ID", this.get32UUID());	//ID
		pd.put("RIGHTS", "");					
		pd.put("LAST_LOGIN", "");				//最后登录时间
		pd.put("IP", "");						//IP
		pd.put("PASSWORD", MD5.md5(pd.getString("PASSWORD")));
		if(null == appuserService.findByUsername(pd)){
			appuserService.saveU(pd);			//判断新增权限
			mv.addObject("msg","success");
		}else{
			mv.addObject("msg","failed");
		}
		mv.setViewName("save_result");
		return mv;
	}
	
	/**判断用户名是否存在
	 * @return
	 */
	@RequestMapping(value="/hasU")
	@ResponseBody
	public Object hasU(){
		Map<String,String> map = new HashMap<String,String>();
		String errInfo = "success";
		PageData pd = new PageData();
		try{
			pd = this.getPageData();
			if(appuserService.findByUsername(pd) != null){
				errInfo = "error";
			}
		} catch(Exception e){
			logger.error(e.toString(), e);
		}
		map.put("result", errInfo);				//返回结果
		return AppUtil.returnObject(new PageData(), map);
	}
	
	/**判断邮箱是否存在
	 * @return
	 */
	@RequestMapping(value="/hasE")
	@ResponseBody
	public Object hasE(){
		Map<String,String> map = new HashMap<String,String>();
		String errInfo = "success";
		PageData pd = new PageData();
		try{
			pd = this.getPageData();
			if(appuserService.findByEmail(pd) != null){
				errInfo = "error";
			}
		} catch(Exception e){
			logger.error(e.toString(), e);
		}
		map.put("result", errInfo);				//返回结果
		return AppUtil.returnObject(new PageData(), map);
	}
	
	/**判断编码是否存在
	 * @return
	 */
	@RequestMapping(value="/hasN")
	@ResponseBody
	public Object hasN(){
		Map<String,String> map = new HashMap<String,String>();
		String errInfo = "success";
		PageData pd = new PageData();
		try{
			pd = this.getPageData();
			if(appuserService.findByNumber(pd) != null){
				errInfo = "error";
			}
		} catch(Exception e){
			logger.error(e.toString(), e);
		}
		map.put("result", errInfo);				//返回结果
		return AppUtil.returnObject(new PageData(), map);
	}
	
	/**删除用户
	 * @param out
	 * @throws Exception 
	 */
	@RequestMapping(value="/deleteU")
	public void deleteU(PrintWriter out) throws Exception{
		PageData pd = this.getPageData();
		pd = this.getPageData();
		Session session = Jurisdiction.getSession();
		User user = (User)session.getAttribute(Const.SESSION_USER);	/*获取当前登陆用户*/
		String USERNAME = user.getUSERNAME();
		FHLOG.save(USERNAME, "删除会员"+pd.getString("userNickName"),super.getIP());
		if(!Jurisdiction.buttonJurisdiction(menuUrl, "del")){return;} //校验权限
		logBefore(logger, Jurisdiction.getUsername()+"删除会员");
		
		appuserService.deleteU(pd);
		out.write("success");
		out.close();
	}
	
	/**修改用户
	 * @param out
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/editU")
	public ModelAndView editU(PrintWriter out) throws Exception{
		PageData pd = new PageData();
		pd = this.getPageData();
		Session session = Jurisdiction.getSession();
		User user = (User)session.getAttribute(Const.SESSION_USER);	/*获取当前登陆用户*/
		String USERNAME = user.getUSERNAME();
		FHLOG.save(USERNAME, "修改会员"+pd.getString("userNickName")+"基本信息",super.getIP());
		if(!Jurisdiction.buttonJurisdiction(menuUrl, "edit")){return null;} //校验权限
		logBefore(logger, Jurisdiction.getUsername()+"修改会员");
		ModelAndView mv = this.getModelAndView();
		if(pd.getString("PASSWORD") != null && !"".equals(pd.getString("PASSWORD"))){
			pd.put("PASSWORD", MD5.md5(pd.getString("PASSWORD")));
		}
		String robotSex = pd.getString("sex");
		/*if(robotSex != null && !"".equals(robotSex)){
			if("男".equals(robotSex)){
				pd.put("robotSex",0 );
			}else if("女".equals(robotSex)){
				pd.put("robotSex",1 );
			}
		}*/
		appuserService.editU(pd);
		mv.addObject("msg","success");
		mv.setViewName("save_result");
		return mv;
	}
	
	/**去修改用户页面
	 * @return
	 */
	@RequestMapping(value="/goEditU")
	public ModelAndView goEditU(){
		if(!Jurisdiction.buttonJurisdiction(menuUrl, "cha")){return null;} //校验权限
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		try {
			/*pd.put("ROLE_ID", "2");
			List<Role> roleList = roleService.listAllRolesByPId(pd);//列出会员组角色
*/			pd = appuserService.findByUserNickname(pd);							//根据ID读取
			mv.setViewName("system/appuser/appuser_edit");
			mv.addObject("msg", "editU");
			mv.addObject("pd", pd);
			/*mv.addObject("roleList", roleList);*/
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}						
		return mv;
	}
	
	/**批量删除
	 * @return
	 */
	@RequestMapping(value="/deleteAllU")
	@ResponseBody
	public Object deleteAllU() {
		if(!Jurisdiction.buttonJurisdiction(menuUrl, "del")){} //校验权限
		logBefore(logger, Jurisdiction.getUsername()+"批量删除会员");
		PageData pd = new PageData();
		Map<String,Object> map = new HashMap<String,Object>();
		try {
			pd = this.getPageData();
			List<PageData> pdList = new ArrayList<PageData>();
			String USER_IDS = pd.getString("USER_IDS");
			if(null != USER_IDS && !"".equals(USER_IDS)){
				String ArrayUSER_IDS[] = USER_IDS.split(",");
				appuserService.deleteAllU(ArrayUSER_IDS);
				pd.put("msg", "ok");
			}else{
				pd.put("msg", "no");
			}
			pdList.add(pd);
			map.put("list", pdList);
		} catch (Exception e) {
			logger.error(e.toString(), e);
		} finally {
			logAfter(logger);
		}
		return AppUtil.returnObject(pd, map);
	}
	
	/**导出会员信息到excel
	 * @return
	 */
	@RequestMapping(value="/excel")
	public ModelAndView exportExcel(){
		logBefore(logger, Jurisdiction.getUsername()+"导出会员资料");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		try{
			if(Jurisdiction.buttonJurisdiction(menuUrl, "cha")){	
				String keywords = pd.getString("keywords");
				if(null != keywords && !"".equals(keywords)){
					pd.put("keywords", keywords.trim());
				}
				String lastLoginStart = pd.getString("lastLoginStart");
				String lastLoginEnd = pd.getString("lastLoginEnd");
				if(lastLoginStart != null && !"".equals(lastLoginStart)){
					pd.put("lastLoginStart", lastLoginStart+" 00:00:00");
				}
				if(lastLoginEnd != null && !"".equals(lastLoginEnd)){
					pd.put("lastLoginEnd", lastLoginEnd+" 00:00:00");
				} 
				Map<String,Object> dataMap = new HashMap<String,Object>();
				List<String> titles = new ArrayList<String>();
				titles.add("用户名"); 		//1
				titles.add("编号");  		//2
				titles.add("姓名");			//3
				titles.add("手机号");		//4
				titles.add("身份证号");		//5
				titles.add("等级");			//6
				titles.add("邮箱");			//7
				titles.add("最近登录");		//8
				titles.add("到期时间");		//9
				titles.add("上次登录IP");	//10
				dataMap.put("titles", titles);
				List<PageData> userList = appuserService.listAllUser(pd);
				List<PageData> varList = new ArrayList<PageData>();
				for(int i=0;i<userList.size();i++){
					PageData vpd = new PageData();
					vpd.put("var1", userList.get(i).getString("USERNAME"));		//1
					vpd.put("var2", userList.get(i).getString("NUMBER"));		//2
					vpd.put("var3", userList.get(i).getString("NAME"));			//3
					vpd.put("var4", userList.get(i).getString("PHONE"));		//4
					vpd.put("var5", userList.get(i).getString("SFID"));			//5
					vpd.put("var6", userList.get(i).getString("ROLE_NAME"));	//6
					vpd.put("var7", userList.get(i).getString("EMAIL"));		//7
					vpd.put("var8", userList.get(i).getString("LAST_LOGIN"));	//8
					vpd.put("var9", userList.get(i).getString("END_TIME"));		//9
					vpd.put("var10", userList.get(i).getString("IP"));			//10
					varList.add(vpd);
				}
				dataMap.put("varList", varList);
				ObjectExcelView erv = new ObjectExcelView();
				mv = new ModelAndView(erv,dataMap);
			}
		} catch(Exception e){
			logger.error(e.toString(), e);
		}
		return mv;
	}
	
	@InitBinder
	public void initBinder(WebDataBinder binder){
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		binder.registerCustomEditor(Date.class, new CustomDateEditor(format,true));
	}
	
}
