<%@ page language="java" errorPage="/sample/common/error.jsp" pageEncoding="UTF-8" contentType="text/html;charset=utf-8" %><%@ include file="/sample/common/taglibs.jsp"%>
<div id="left">
<style type="text/css">
<!--
.menus {
        background-image:
                url("<c:url value='/sample/images/top_menus.png'/>");
}

.depth01 a:hover {
        background: #dadada
                url("<c:url value='/sample/images/menu_hover.png'/>") left top
                no-repeat;
}
-->
    </style>
    <table border="0" cellpadding="0" cellspacing="0" bgcolor="#eeeeee">
        <tr>
            <td>
                <table width="177" border="0" cellpadding="0" cellspacing="0" bgcolor="#FFFFFF">
                    <tr>
                        <td height="23" bgcolor="#eeeeee" class="menus"></td>
                    </tr>
                    <tr>
                        <td valign="top" bgcolor="#eeeeee" class="depth01">
                            <a href="${ctx}/coreMovieFinder.do?method=list">Core</a>
                        </td>
                    </tr>
<!--Add new menu here-->
<!--remoting-menu-START-->
                    <tr>
                        <td valign="top" bgcolor="#eeeeee" class="depth01">
                            <a href="${ctx}/remotingMovieFinder.do?method=list">Remoting</a>
                        </td>
                    </tr>
<!--remoting-menu-END-->
                </table>
            </td>
        </tr>
    </table>
</div>
