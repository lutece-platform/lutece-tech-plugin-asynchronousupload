<%@ page errorPage="../../ErrorPage.jsp" %>
<jsp:useBean id="asynchronousUploadApp" scope="request" class="fr.paris.lutece.plugins.asynchronousupload.web.AsynchronousUploadApp" />
<% asynchronousUploadApp.doRetrieveAsynchronousUploadedFile( request, response );%>