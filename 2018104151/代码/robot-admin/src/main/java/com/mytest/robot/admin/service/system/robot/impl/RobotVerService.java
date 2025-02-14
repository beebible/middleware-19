package com.zoneyet.robot.admin.service.system.robot.impl;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.tools.ant.types.resources.selectors.Date;
import org.springframework.stereotype.Service;

import com.zoneyet.robot.admin.dao.DaoSupport;
import com.zoneyet.robot.admin.entity.Page;
import com.zoneyet.robot.admin.service.system.robot.RobotVerManager;
import com.zoneyet.robot.admin.util.PageData;


/** 
 * 说明： 机器人版本
 * 创建人：FH Q313596790
 * 创建时间：2018-03-12
 * @version
 */
@Service("robotverService")
public class RobotVerService implements RobotVerManager{

	@Resource(name = "daoSupport")
	private DaoSupport dao;
	
	/**新增
	 * @param pd
	 * @throws Exception
	 */
	public void save(PageData pd)throws Exception{
		Set<String> set =pd.keySet();
		//pd.put("createtime", new Date());
		dao.save("RobotVerMapper.save", pd);
	}
	
	/**删除
	 * @param pd
	 * @throws Exception
	 */
	public Boolean delete(PageData pd)throws Exception{
		List<PageData> data=(List<PageData>)dao.findForList("RobotVerMapper.checkVer", pd);
		if(data.size()>0){
			return false;
		}else {
			dao.delete("RobotVerMapper.delete", pd);
			return true;
		}
		
	
	}
	
	/**修改
	 * @param pd
	 * @throws Exception
	 */
	public void edit(PageData pd)throws Exception{
		Set<String> set =pd.keySet();
		for (String key : set) {
			if("true".equals(pd.get(key))){
				pd.put(key, 1);
			}else if("false".equals(pd.get(key))){
				pd.put(key, 0);
			}
		}
		dao.update("RobotVerMapper.edit", pd);
		dao.update("RobotVerMapper.edit2", pd);
		dao.update("RobotVerMapper.edit3", pd);
	}
	
	/**列表
	 * @param page
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public List<PageData> list(Page page)throws Exception{
		return (List<PageData>)dao.findForList("RobotVerMapper.datalistPage", page);
	}
	
	/**列表(全部)hhh
	 * @param pd
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public List<PageData> listAll(PageData pd)throws Exception{
		return (List<PageData>)dao.findForList("RobotVerMapper.listAll", pd);
	}
	
	/**通过id获取数据
	 * @param pd
	 * @throws Exception
	 */
	public PageData findById(PageData pd)throws Exception{
		return (PageData)dao.findForObject("RobotVerMapper.findById", pd);
	}
	
	/**批量删除
	 * @param ArrayDATA_IDS
	 * @throws Exception
	 */
	public void deleteAll(String[] ArrayDATA_IDS)throws Exception{
		dao.delete("RobotVerMapper.deleteAll", ArrayDATA_IDS);
	}
	
}

