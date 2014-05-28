<%@ page errorPage="../../ErrorPagePortal.jsp" trimDirectiveWhitespaces="true" %>

<jsp:useBean id="asynchronousUploadApp" scope="request" class="fr.paris.lutece.plugins.asynchronousupload.web.AsynchronousUploadApp" />

<%= asynchronousUploadApp.getMainUploadJs( request ) %>