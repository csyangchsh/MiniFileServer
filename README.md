# MiniFileServer

A mini http server to share file in LAN

## Intention

This project is for learning and practice. [practices/MiniFileServer](https://github.com/csyangchsh/practices/tree/master/MiniFileServer) won't update anymore.

Current implementation is based Apache Commons FileUpload and HttpCompnent Core.

Because FileUpload only supports Servlets and Portlets, I extended FileUpload and RequestContext. (Note: need to add Servlet API as dependency, FileUpload depends on it.)

## TODO

1. [] Remove all framework/lib dependencies
	* [x] Removed apache http client
2. [] Try Java NIO and thread pool
3. [] Add progress bar, page css



