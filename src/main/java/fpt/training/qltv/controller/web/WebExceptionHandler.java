package fpt.training.qltv.controller.web;

import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.exception.common.FileAccessDeniedException;
import fpt.training.qltv.exception.common.FileUploadException;
import fpt.training.qltv.exception.common.ResourceNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackages = "fpt.training.qltv.controller.web")
public class WebExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(FileAccessDeniedException.class)
    public String handleAccessDenied(FileAccessDeniedException ex, Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model) {
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/400";
    }

    @ExceptionHandler(FileUploadException.class)
    public String handleFileUpload(FileUploadException ex, Model model) {
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", "Lỗi hệ thống, vui lòng thử lại sau");
        System.err.println("Unhandled exception: " + ex.getMessage());

        return "error/500";
    }
}
