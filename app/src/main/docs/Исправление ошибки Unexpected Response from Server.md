## Исправление ошибки Unexpected Response from Server
При попытке сделать pull Android Studio может выдать ошибку такого типа:
>Invocation failed Unexpected Response from Server:  Unauthorized
>        java.lang.RuntimeException: Invocation failed Unexpected Response from Server:  Unauthorized
>        at org.jetbrains.git4idea.nativessh.GitNativeSshAskPassXmlRpcClient.handleInput(GitNativeSshAskPassXmlRpcClient.java:34)
>        at org.jetbrains.git4idea.nativessh.GitNativeSshAskPassApp.main(GitNativeSshAskPassApp.java:30)
>        Caused by: java.io.IOException: Unexpected Response from Server:  Unauthorized
>        at org.apache.xmlrpc.LiteXmlRpcTransport.sendRequest(LiteXmlRpcTransport.java:231)
>        at org.apache.xmlrpc.LiteXmlRpcTransport.sendXmlRpc(LiteXmlRpcTransport.java:90)
>        at org.apache.xmlrpc.XmlRpcClientWorker.execute(XmlRpcClientWorker.java:72)
>        at org.apache.xmlrpc.XmlRpcClient.execute(XmlRpcClient.java:194)
>        at org.apache.xmlrpc.XmlRpcClient.execute(XmlRpcClient.java:185)
>        at org.apache.xmlrpc.XmlRpcClient.execute(XmlRpcClient.java:178)
В таком случае необходимо поставить галочку:
AndroidStudio -> File -> Settings -> Version Control -> Git ->  Use credential helper
Среда предложит авторизоваться в своём аккаунте в GitHub используя токен или через браузер.
Можно выбрать браузер. Он автоматически откроет нужную страницу с авторизацией. 
После ввода своих логина и пароля от GitHub, они запомнятся в среде Android Studio.
