<%@ page language="java" contentType="text/html; charset=UTF-8"  pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<html lang="en">
<head>
<script type="text/javascript" src="/webjars/jquery/3.3.1/jquery.min.js"></script>
</head>
<body>
    <div>
        <div>
            <h2>Book Info</h2>
             
            <div>
            	<form action="">
            		ISBN : <input type="text" id="isbn" name="isbn"/>
            		<input type="button" name="search" id="search" value="search">
            	</form>
            
            </div>
            	<dl>
            		<dt><strong>서명(부제목)</strong></dt><dd><span id="title"></span> <span id="subTitle"></span></dd>
            	</dl>
            	<dl>
            		<dt><strong>저자</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>출판사</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>출간일</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>정가</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>쪽수,무게,크기</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>ISBN13</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>ISBN10</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>관련분류</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>책소개</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>저자소개</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>목차</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>책속으로</strong></dt><dd id="author"></dd>
            	</dl>
            	<dl>
            		<dt><strong>출판사리뷰</strong></dt><dd id="author"></dd>
            	</dl>
            
            	
            <div>
            	
            </div>
        </div>
    </div>
<script>
   $(function() {
       
   });
</script>
</body>
</html>