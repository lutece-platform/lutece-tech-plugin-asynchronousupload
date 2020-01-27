<%@ page errorPage="../../ErrorPagePortal.jsp" trimDirectiveWhitespaces="true" contentType="application/javascript; charset=UTF-8" %>

<jsp:useBean id="asynchronousUploadApp" scope="request" class="fr.paris.lutece.plugins.asynchronousupload.web.AsynchronousUploadApp" />

<%= asynchronousUploadApp.getMainUploadJs( request ) %>