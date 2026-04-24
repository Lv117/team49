package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.DictionaryInfo;
import cn.edu.sdu.java.server.models.MenuInfo;
import cn.edu.sdu.java.server.models.ModifyLog;
import cn.edu.sdu.java.server.models.SystemInfo;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.repositorys.DictionaryInfoRepository;
import cn.edu.sdu.java.server.repositorys.MenuInfoRepository;
import cn.edu.sdu.java.server.repositorys.ModifyLogRepository;
import cn.edu.sdu.java.server.repositorys.SystemInfoRepository;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SystemService 系统服务行数
 */
@Service
public class SystemService {
    private final DictionaryInfoRepository dictionaryInfoRepository; //数据数据操作自动注入
    private final SystemInfoRepository systemInfoRepository; //数据数据操作自动注入
    private final MenuInfoRepository menuInfoRepository;

    private final ModifyLogRepository modifyLogRepository; //数据数据操作自动注入
    public SystemService(DictionaryInfoRepository dictionaryInfoRepository, SystemInfoRepository systemInfoRepository, MenuInfoRepository menuInfoRepository, ModifyLogRepository modifyLogRepository) {
        this.dictionaryInfoRepository = dictionaryInfoRepository;
        this.systemInfoRepository = systemInfoRepository;
        this.menuInfoRepository = menuInfoRepository;
        this.modifyLogRepository = modifyLogRepository;
    }
    /**
     *  initDictionary 初始数据字典 在系统初始时将数据字典加载内存，业务处理是直接从内从中获取数数据字典列表和数据字典名称
     */
    public void initDictionary() {
        List<OptionItem> itemList;
        OptionItem item;
        Map<String, String> sMap;
        String value;
        Map<String, List<OptionItem>> dictListMap = ComDataUtil.getInstance().getDictListMap();
        Map<String, Map<String, String>> dictMapMap = ComDataUtil.getInstance().getDictMapMap();
        List<DictionaryInfo>  dList =dictionaryInfoRepository.findRootList();
        List<DictionaryInfo> sList;
        for(DictionaryInfo df : dList) {
            value = df.getValue();
            sMap = new HashMap<String, String>();
            dictMapMap.put(value, sMap);
            itemList = new ArrayList<>();
            dictListMap.put(value, itemList);
            sList = dictionaryInfoRepository.findByPid(df.getId());
            for (DictionaryInfo d : sList) {
                sMap.put(d.getValue(), d.getLabel());
                item = new OptionItem(d.getId(), d.getValue(), d.getLabel());
                itemList.add(item);
            }
        }
        ComDataUtil pi = ComDataUtil.getInstance();
        pi.setDictListMap(dictListMap);
        pi.setDictMapMap(dictMapMap);
    }
    public void initSystem() {
        List<SystemInfo> sList = systemInfoRepository.findAll();
        Map<String,String> map = new HashMap<>();
        for(SystemInfo s:sList) {
            map.put(s.getName(),s.getValue());
        }
        ComDataUtil pi = ComDataUtil.getInstance();
        pi.setSystemMap(map);
        
        // 创新创业板块菜单初始化
        MenuInfo innovationMenu = ensureMenu("innovation", "创新创业", "1,2,3", null);
        if (innovationMenu != null) {
            ensureMenu("innovation-project", "创业实践", "1,2,3", innovationMenu.getId());
            ensureMenu("competition", "学科竞赛", "1,2,3", innovationMenu.getId());
            ensureMenu("achievement", "科研成果", "1,2,3", innovationMenu.getId());
        }
        
        // 清理旧的 innovation-practice 菜单
        menuInfoRepository.findByName("innovation-practice").ifPresent(m -> menuInfoRepository.delete(m));
        
        ensureMenuLeaf("honor-panel", "荣誉奖励", "1,2,3");
        
        // Remove old innovation-panel to prevent duplicates
        menuInfoRepository.findByName("innovation-panel").ifPresent(m -> menuInfoRepository.delete(m));
    }

    private MenuInfo ensureMenu(String name, String title, String userTypeIds, Integer pid) {
        if (name == null || name.isEmpty()) return null;
        Optional<MenuInfo> byName = menuInfoRepository.findByName(name);
        if (byName.isPresent()) {
            MenuInfo menuInfo = byName.get();
            boolean changed = false;
            if (!Objects.equals(menuInfo.getPid(), pid)) {
                menuInfo.setPid(pid);
                changed = true;
            }
            if (title != null && !title.equals(menuInfo.getTitle())) {
                menuInfo.setTitle(title);
                changed = true;
            }
            if (userTypeIds != null && !userTypeIds.equals(menuInfo.getUserTypeIds())) {
                menuInfo.setUserTypeIds(userTypeIds);
                changed = true;
            }
            if (changed) {
                menuInfo = menuInfoRepository.save(menuInfo);
            }
            return menuInfo;
        }

        Integer maxId = menuInfoRepository.findMaxId();
        int nextId = (maxId == null ? 0 : maxId) + 1;
        MenuInfo menuInfo = new MenuInfo();
        menuInfo.setId(nextId);
        menuInfo.setPid(pid);
        menuInfo.setName(name);
        menuInfo.setTitle(title);
        menuInfo.setUserTypeIds(userTypeIds);
        return menuInfoRepository.save(menuInfo);
    }

    private void ensureMenuLeaf(String name, String title, String userTypeIds) {
        if (name == null || name.isEmpty()) {
            return;
        }
        Optional<MenuInfo> byName = menuInfoRepository.findByName(name);
        if (byName.isPresent()) {
            MenuInfo menuInfo = byName.get();
            boolean changed = false;
            if (menuInfo.getPid() != null) {
                menuInfo.setPid(null);
                changed = true;
            }
            if (title != null && !title.equals(menuInfo.getTitle())) {
                menuInfo.setTitle(title);
                changed = true;
            }
            if (userTypeIds != null && !userTypeIds.equals(menuInfo.getUserTypeIds())) {
                menuInfo.setUserTypeIds(userTypeIds);
                changed = true;
            }
            if (changed) {
                menuInfoRepository.save(menuInfo);
            }
            cleanDuplicateRootMenusByTitle(name, title);
            return;
        }

        List<MenuInfo> sameTitleRoots = title == null ? Collections.emptyList() : menuInfoRepository.findRootByTitle(title);
        if (sameTitleRoots != null && !sameTitleRoots.isEmpty()) {
            MenuInfo target = sameTitleRoots.stream()
                    .filter(m -> name.equals(m.getName()))
                    .findFirst()
                    .orElse(sameTitleRoots.get(0));

            boolean changed = false;
            if (target.getPid() != null) {
                target.setPid(null);
                changed = true;
            }
            if (!name.equals(target.getName())) {
                target.setName(name);
                changed = true;
            }
            if (userTypeIds != null && !userTypeIds.equals(target.getUserTypeIds())) {
                target.setUserTypeIds(userTypeIds);
                changed = true;
            }
            if (title != null && !title.equals(target.getTitle())) {
                target.setTitle(title);
                changed = true;
            }
            if (changed) {
                menuInfoRepository.save(target);
            }
            cleanDuplicateRootMenusByTitle(name, title);
            return;
        }

        Integer maxId = menuInfoRepository.findMaxId();
        int nextId = (maxId == null ? 0 : maxId) + 1;
        MenuInfo menuInfo = new MenuInfo();
        menuInfo.setId(nextId);
        menuInfo.setPid(null);
        menuInfo.setName(name);
        menuInfo.setTitle(title);
        menuInfo.setUserTypeIds(userTypeIds);
        menuInfoRepository.save(menuInfo);
    }

    private void cleanDuplicateRootMenusByTitle(String keepName, String title) {
        if (title == null || title.isEmpty()) {
            return;
        }
        List<MenuInfo> roots = menuInfoRepository.findRootByTitle(title);
        if (roots == null || roots.size() <= 1) {
            return;
        }
        MenuInfo keep = roots.stream()
                .filter(m -> keepName != null && keepName.equals(m.getName()))
                .findFirst()
                .orElse(roots.get(0));
        for (MenuInfo m : roots) {
            if (m.getId() == null || keep.getId() == null) {
                continue;
            }
            if (m.getId().equals(keep.getId())) {
                continue;
            }
            int childCount = menuInfoRepository.countMenuInfoByPid(m.getId());
            if (childCount == 0) {
                menuInfoRepository.delete(m);
            }
        }
    }
    public void modifyLog(Object o, boolean isCreate) {
        String info = CommonMethod.ObjectToJSon(o);
        if(info == null)
            return;
        String tableName = o.getClass().getName();
        int index = tableName.lastIndexOf('.');
        if(index > 0) {
            tableName = tableName.substring(index+1);
        }
        ModifyLog l = new ModifyLog();
        l.setTableName(tableName);
        if(isCreate)
            l.setType("0");
        else
            l.setType("1");
        l.setInfo(info);
        l.setOperateTime(DateTimeTool.parseDateTime(new Date()));
        l.setOperatorId(CommonMethod.getPersonId());
        modifyLogRepository.save(l);
    }
}

