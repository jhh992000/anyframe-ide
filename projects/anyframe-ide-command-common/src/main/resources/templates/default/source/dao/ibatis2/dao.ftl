<#assign pojoNameLower = pojo.shortName.substring(0,1).toLowerCase()+pojo.shortName.substring(1)>
<#assign setIdMethodName = 'set' + pojo.getPropertyName(pojo.identifierProperty)>
<#assign valueType = pojo.getJavaTypeName(pojo.identifierProperty, jdk5)>	
package ${package}.service.impl;

import java.util.List;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import org.anyframe.pagination.Page;
import org.anyframe.datatype.SearchVO;
import com.ibatis.sqlmap.client.SqlMapClient;

import ${pojo.packageName}.${pojo.shortName};
<#if c2j.isComponent(pojo.identifierProperty) >
import ${pojo.packageName}.${pojo.getJavaTypeName(pojo.identifierProperty, jdk5)};
<#elseif !data.isPrimitive(valueType)>
  <#foreach field in pojo.getAllPropertiesIterator()>	  
    <#if field.equals(pojo.identifierProperty)>
import ${field.value.type.returnedClass.name};
    </#if>
  </#foreach>
</#if>

/**
 * ${pojo.shortName}Dao implementation class with iBatis2
 *
 */
@Repository("${pojoNameLower}Dao")
public class ${pojo.shortName}Dao {
  
  	@Value("${r"#{contextProperties['pageSize'] ?: 10}"}")
	int pageSize;

	@Value("${r"#{contextProperties['pageUnit'] ?: 10}"}")
	int pageUnit;
		
	@Inject
	protected SqlMapClient sqlMap = null;
	
	public void create(${pojo.shortName} ${pojoNameLower}) throws Exception {
		sqlMap.insert("insert${pojo.shortName}", ${pojoNameLower});
	}
	
	public void update(${pojo.shortName} ${pojoNameLower}) throws Exception {
		sqlMap.update("update${pojo.shortName}", ${pojoNameLower});
	}
	
	public void remove(${pojo.getJavaTypeName(pojo.identifierProperty, jdk5)} ${pojo.identifierProperty.name}) throws Exception {
		sqlMap.delete("delete${pojo.shortName}", ${pojo.identifierProperty.name});
	}
	
	public ${pojo.shortName} get(${pojo.getJavaTypeName(pojo.identifierProperty, jdk5)} ${pojo.identifierProperty.name}) throws Exception {
		return (${pojo.shortName}) sqlMap.queryForObject("get${pojo.shortName}", ${pojo.identifierProperty.name});
	}

	@SuppressWarnings("unchecked")
	public Page getPagingList(SearchVO searchVO) throws Exception  {
      int pageIndex = searchVO.getPageIndex();		
	 
	  List<${pojo.shortName}> list=sqlMap.queryForList("get${pojo.shortName}List", pageSize*(pageIndex-1), pageSize);
	  int rowCount= (Integer)sqlMap.queryForObject("get${pojo.shortName}ListCnt");
	  
	  return new Page(list, pageIndex, rowCount, pageUnit, pageSize);
  }	
}
