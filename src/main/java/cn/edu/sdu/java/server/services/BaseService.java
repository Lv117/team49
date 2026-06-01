package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.MyTreeNode;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BaseService {
    private static final Logger log = LoggerFactory.getLogger(BaseService.class);
    @Value("${attach.folder}")    //环境配置变量获取
    private String attachFolder;  //服务器端数据存储
    @Value("${spring.datasource.username}")    //环境配置变量获取
    private String dataBaseUserName;  //服务器端数据存储
    private final PersonRepository personRepository;
    private final PasswordEncoder encoder;  //密码服务自动注入
    private final UserRepository userRepository;  //用户数据操作自动注入
    private final MenuInfoRepository menuInfoRepository; //菜单数据操作自动注入
    private final DictionaryInfoRepository dictionaryInfoRepository;  //数据字典数据操作自动注入
    private final UserTypeRepository userTypeRepository;   //用户类型数据操作自动注入

    public BaseService(PersonRepository personRepository, PasswordEncoder encoder, UserRepository userRepository, MenuInfoRepository menuInfoRepository, DictionaryInfoRepository dictionaryInfoRepository, UserTypeRepository userTypeRepository) {
        this.personRepository = personRepository;
        this.encoder = encoder;
        this.userRepository = userRepository;
        this.menuInfoRepository = menuInfoRepository;
        this.dictionaryInfoRepository = dictionaryInfoRepository;
        this.userTypeRepository = userTypeRepository;
    }

    public DataResponse getDataBaseUserName() {
        return CommonMethod.getReturnData(dataBaseUserName);
    }
    /**
     *  getDictionaryTreeNode 获取数据字典节点树根节点
     * @return MyTreeNode 数据字典树根节点
     */
    public List<MyTreeNode> getDictionaryTreeNodeList() {
        List<MyTreeNode> childList = new ArrayList<MyTreeNode>();
        List<DictionaryInfo> sList = dictionaryInfoRepository.findRootList();
        if(sList == null)
            return childList;
        for (DictionaryInfo dictionaryInfo : sList) {
            childList.add(getDictionaryTreeNode(null, dictionaryInfo, null));
        }
        return childList;
    }

    /**
     * 获得数据字典的MyTreeNode
     */
    public MyTreeNode getDictionaryTreeNode( Integer pid, DictionaryInfo d,String parentTitle) {
        MyTreeNode  node = new MyTreeNode(d.getId(),d.getValue(),d.getLabel(),null);
        node.setLabel(d.getValue()+"-"+d.getLabel());
        node.setParentTitle(parentTitle);
        node.setPid(pid);
        List<MyTreeNode> childList = new ArrayList<MyTreeNode>();
        node.setChildren(childList);
        List<DictionaryInfo> sList = dictionaryInfoRepository.findByPid(d.getId());
        if(sList == null)
            return node;
        for (DictionaryInfo dictionaryInfo : sList) {
            childList.add(getDictionaryTreeNode(node.getId(), dictionaryInfo, node.getValue()));
        }
        return node;
    }

    /**
     * MyTreeNode getMenuTreeNode(Integer userTypeId) 获得角色的菜单树根节点
     */
    public List<MyTreeNode> getMenuTreeNodeList() {
        List<MyTreeNode> childList = new ArrayList<MyTreeNode>();
        List<MenuInfo> sList = menuInfoRepository.findByUserTypeIds("");
        if(sList == null)
            return childList;
        for (MenuInfo menuInfo : sList) {
            childList.add(getMenuTreeNode(null, menuInfo, ""));
        }
        return childList;
    }
    /**
     * MyTreeNode getMenuTreeNode(Integer userTypeId) 获得角色的某个菜单的菜单树根节点
     */
    public MyTreeNode getMenuTreeNode(Integer pid, MenuInfo d,String parentTitle) {
        MyTreeNode  node = new MyTreeNode(d.getId(),d.getName(),d.getTitle(),null);
        node.setLabel(d.getId()+"-"+d.getTitle());
        node.setUserTypeIds(d.getUserTypeIds());
        node.setParentTitle(parentTitle);
        node.setPid(pid);
        List<MyTreeNode> childList = new ArrayList<MyTreeNode>();
        node.setChildren(childList);
        List<MenuInfo> sList = menuInfoRepository.findByUserTypeIds("",d.getId());
        if(sList == null)
            return node;
        for (MenuInfo menuInfo : sList) {
            childList.add(getMenuTreeNode(node.getId(), menuInfo, node.getTitle()));
        }
        return node;
    }



    public List<Map<String, Object>> getMenuList(Integer userTypeId, Integer pid) {
        List<Map<String, Object>> sList = new ArrayList<>();
        Map<String, Object> ms;
        List<MenuInfo> msList = menuInfoRepository.findByUserTypeIds(userTypeId + "", pid);
        String name, path;
        if (msList != null) {
            for (MenuInfo info : msList) {
                ms = new HashMap<>();
                name = info.getName();
                if(name!= null&& !name.isEmpty()) {
                    path = name.substring(0,1).toLowerCase()+ name.substring(1);
                }else {
                    path = "";
                }
                ms.put("id", info.getId());
                ms.put("name", name);
                ms.put("path", path);
                ms.put("title", info.getTitle());
                ms.put("sList", getMenuList(userTypeId, info.getId()));
                sList.add(ms);
            }
        }
        return sList;
    }

    public DataResponse getMenuList(DataRequest dataRequest) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        Integer userTypeId = dataRequest.getInteger("userTypeId");
        if (userTypeId == null) {
            Integer personId = CommonMethod.getPersonId();
            if (personId == null)
                return CommonMethod.getReturnData(dataList);
            User currentUser = userRepository.findById(personId)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.ACCOUNT_NOT_FOUND, "当前账户不存在"));
            if (currentUser.getUserType() == null || currentUser.getUserType().getId() == null) {
                throw new BusinessException(ErrorCodes.MENU_USER_TYPE_MISSING, "当前账户未配置角色，无法加载菜单");
            }
            userTypeId = currentUser.getUserType().getId();
        }
        List<MenuInfo> mList = menuInfoRepository.findByUserTypeIds(userTypeId + "");

        Map<String, Object> m;
        List<Map<String, Object>> sList;
        String name, path;
        for (MenuInfo info : mList) {
            m = new HashMap<>();
            name = info.getName();
            if(name!= null&& !name.isEmpty()) {
                path = name.substring(0,1).toLowerCase()+ name.substring(1);
            } else {
                path = "";
            }
            m.put("id", info.getId());
            m.put("path", path);
            m.put("name", name);
            m.put("title", info.getTitle());
            sList = getMenuList(userTypeId, info.getId());
            m.put("sList", sList);
            dataList.add(m);
        }
        return CommonMethod.getReturnData(dataList);
    }



    public OptionItemList getRoleOptionItemList(@Valid @RequestBody DataRequest dataRequest) {
        List<UserType> uList = userTypeRepository.findAll();
        List<OptionItem> itemList = new ArrayList<>();
        for (UserType ut : uList) {
            itemList.add(new OptionItem(ut.getId(), null, ut.getName()));
        }
        return new OptionItemList(0, itemList);
    }


    public DataResponse menuDelete(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        int count  = menuInfoRepository.countMenuInfoByPid(id);
        if(count > 0) {
            return CommonMethod.getReturnMessageError("存在子菜单，不能删除！");
        }
        Optional<MenuInfo> op = menuInfoRepository.findById(id);
        op.ifPresent(menuInfoRepository::delete);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse menuSave(DataRequest dataRequest) {
        Integer editType = dataRequest.getInteger("editType");
        Map<String, Object> node = dataRequest.getMap("node");
        Integer pid = CommonMethod.getInteger(node,"pid");
        Integer id = CommonMethod.getInteger(node,"id");
        String name = CommonMethod.getString(node,"value");
        String title = CommonMethod.getString(node,"title");
        String userTypeIds = CommonMethod.getString(node,"userTypeIds");
        Optional<MenuInfo> op;
        MenuInfo m = null;
        if (id != null) {
            op = menuInfoRepository.findById(id);
            if(op.isPresent()) {
                if(editType == 0 || editType == 1)
                    return CommonMethod.getReturnMessageError("主键已经存在，不能添加");
                m = op.get();
            }
        }
        if (m == null)
            m = new MenuInfo();
        m.setId(id);
        m.setTitle(title);
        m.setName(name);
        m.setPid(pid);
        m.setUserTypeIds(userTypeIds);
        menuInfoRepository.save(m);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse deleteDictionary(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        int count = dictionaryInfoRepository.countDictionaryInfoByPid(id);
        if(count > 0) {
            return CommonMethod.getReturnMessageError("存在数据项，不能删除！");
        }
        Optional<DictionaryInfo> op = dictionaryInfoRepository.findById(id);
        op.ifPresent(dictionaryInfoRepository::delete);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse dictionarySave(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        Integer pid = dataRequest.getInteger("pid");
        String value = dataRequest.getString("value");
        String title = dataRequest.getString("title");
        DictionaryInfo m = null;
        if(id != null) {
            Optional<DictionaryInfo> op = dictionaryInfoRepository.findById(id);
            if (op.isPresent()) {
                m = op.get();
            }
        }
        if(m == null) {
            m = new DictionaryInfo();
            m.setPid(pid);
        }
        m.setLabel(title);
        m.setValue(value);
        dictionaryInfoRepository.save(m);
        return CommonMethod.getReturnMessageOK();
    }

    public OptionItemList getDictionaryOptionItemList(DataRequest dataRequest) {
        String code = dataRequest.getString("code");
        List<DictionaryInfo> dList = dictionaryInfoRepository.getDictionaryInfoList(code);
        OptionItem item;
        List<OptionItem> itemList = new ArrayList<>();
        for (DictionaryInfo d : dList) {
            itemList.add(new OptionItem(d.getId(), d.getValue(), d.getLabel()));
        }
        return new OptionItemList(0, itemList);
    }

    public ResponseEntity<StreamingResponseBody> getFileByteData(DataRequest dataRequest) {
        String fileName = dataRequest.getString("fileName");
        try {
            File file = new File(attachFolder + fileName);
            int len = (int) file.length();
            byte[] data = new byte[len];
            FileInputStream in = new FileInputStream(file);
            int size = in.read(data);
            in.close();
            MediaType mType = new MediaType(MediaType.APPLICATION_OCTET_STREAM);
            StreamingResponseBody stream = outputStream -> {
                outputStream.write(data);
            };
            return ResponseEntity.ok()
                    .contentType(mType)
                    .body(stream);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ResponseEntity.internalServerError().build();
    }


    public DataResponse uploadPhoto(byte[] barr, String remoteFile) {
        try {
            // 【关键修复】解码路径
            if (remoteFile != null && remoteFile.contains("%")) {
                remoteFile = java.net.URLDecoder.decode(remoteFile, "UTF-8");
            }
            
            File targetFile = new File(attachFolder + remoteFile);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            OutputStream os = new FileOutputStream(targetFile);
            os.write(barr);
            os.close();
            return CommonMethod.getReturnMessageOK();
        } catch (Exception e) {
            log.error("文件上传失败: " + e.getMessage());
            return CommonMethod.getReturnMessageError("上传错误: " + e.getMessage());
        }
    }
    public ResponseEntity<StreamingResponseBody> getBlobByteData(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        try {
            byte [] data;
            Optional<Person> op = personRepository.findById(personId);
            if(op.isPresent()) {
                Person p = op.get();
                data = p.getPhoto();
                if(data == null)
                    return ResponseEntity.notFound().build();
            }else {
                return ResponseEntity.internalServerError().build();
            }
            MediaType mType = new MediaType(MediaType.APPLICATION_OCTET_STREAM);
            StreamingResponseBody stream = outputStream -> {
                outputStream.write(data);
            };
            return ResponseEntity.ok()
                    .contentType(mType)
                    .body(stream);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ResponseEntity.internalServerError().build();
    }

    public DataResponse uploadPhotoBlob(byte[] barr,String personId ) {
        try {
            Optional<Person> op = personRepository.findById(Integer.parseInt(personId));
            if(op.isEmpty())
                return CommonMethod.getReturnMessageError("人员不存在！");
            Person p = op.get();
            p.setPhoto(barr);
            personRepository.save(p);
            return CommonMethod.getReturnMessageOK();
        } catch (Exception e) {
            return CommonMethod.getReturnMessageError("上传错误");
        }
    }

    public DataResponse updatePassword(DataRequest dataRequest) {
        String oldPassword = dataRequest.getString("oldPassword");  //获取oldPassword
        String newPassword = dataRequest.getString("newPassword");  //获取newPassword
        Integer currentPersonId = CommonMethod.getPersonId();
        if (currentPersonId == null) {
            throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
        }
        User u = userRepository.findById(currentPersonId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ACCOUNT_NOT_FOUND, "账户不存在"));
        if (!encoder.matches(oldPassword, u.getPassword())) {
            throw new BusinessException(ErrorCodes.PASSWORD_OLD_INVALID, "原始密码不正确");
        }
        u.setPassword(encoder.encode(newPassword));
        userRepository.save(u);
        return CommonMethod.getReturnMessageOK();
    }


    @PostMapping("/uploadHtmlString")
    @PreAuthorize(" hasRole('ADMIN') ")
    public DataResponse uploadHtmlString(DataRequest dataRequest) {
        String str = dataRequest.getString("html");
        String html = new String(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8)));
        int htmlCount = ComDataUtil.getInstance().addHtmlString(html);
        return CommonMethod.getReturnData(htmlCount);
    }

    public ResponseEntity<StreamingResponseBody> htmlGetBaseHtmlPage(HttpServletRequest request) {
        String htmlCountStr = request.getParameter("htmlCount");
        int htmlCount = Integer.parseInt(htmlCountStr);
        String html = ComDataUtil.getInstance().getHtmlString(htmlCount);
        MediaType mType = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);
        try {
            byte[] data = html.getBytes();
            StreamingResponseBody stream = outputStream -> {
                outputStream.write(data);
            };
            return ResponseEntity.ok()
                    .contentType(mType)
                    .body(stream);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }



    //  Web 请求
    public DataResponse getPhotoImageStr(DataRequest dataRequest) {
        String fileName = dataRequest.getString("fileName");
        String str = "";
        try {
            File file = new File(attachFolder + fileName);
            int len = (int) file.length();
            byte[] data = new byte[len];
            FileInputStream in = new FileInputStream(file);
            len = in.read(data);
            in.close();
            String imgStr = "data:image/png;base64,";
            String s = new String(Base64.getEncoder().encode(data));
            imgStr = imgStr + s;
            return CommonMethod.getReturnData(imgStr);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return CommonMethod.getReturnMessageError("下载错误！");
    }

    public DataResponse uploadPhotoWeb(Map<String, Object> pars, MultipartFile file) {
        try {
            String remoteFile = CommonMethod.getString(pars, "remoteFile");
            // 【关键修复】前端传来的路径包含 URL 编码（如 %2F），必须还原为 /
            if (remoteFile != null && remoteFile.contains("%")) {
                remoteFile = java.net.URLDecoder.decode(remoteFile, "UTF-8");
            }

            File targetFile = new File(attachFolder + remoteFile);
            // 确保父目录存在（解决少一层文件夹的问题）
            if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            // 使用 transferTo 自动保存，避免手动读写流
            file.transferTo(targetFile);
            return CommonMethod.getReturnMessageOK();
        } catch (Exception e) {
            log.error("文件上传失败: " + e.getMessage());
            return CommonMethod.getReturnMessageError("上传错误: " + e.getMessage());
        }
    }
    public DataResponse uploadPhotoBlobWeb(Map<String,Object> pars, MultipartFile file) {
        try {
            String personId = CommonMethod.getString(pars, "remoteFile");
            InputStream in = file.getInputStream();
            int size = (int) file.getSize();
            byte[] data = new byte[size];
            int len =  in.read(data);
            in.close();
            return uploadPhotoBlob(data,personId);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 根据账号列表批量查询 (num → name) 映射
     */
    public DataResponse getPersonNamesByAccounts(DataRequest dataRequest) {
        List<?> accounts = dataRequest.getList("accounts");
        List<String> numList = new ArrayList<>();
        for (Object acc : accounts) {
            if (acc != null) {
                numList.add(acc.toString());
            }
        }
        Map<String, String> result = new HashMap<>();
        if (!numList.isEmpty()) {
            List<Person> persons = personRepository.findByNumIn(numList);
            for (Person p : persons) {
                result.put(p.getNum(), p.getName());
            }
        }
        return CommonMethod.getReturnData(result);
    }

}
