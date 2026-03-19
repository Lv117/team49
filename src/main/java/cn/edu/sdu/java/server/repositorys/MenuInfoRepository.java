package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.MenuInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuInfoRepository extends JpaRepository<MenuInfo, Integer> {

    // 查询所有顶级菜单（pid 为 null）
    @Query("SELECT m FROM MenuInfo m WHERE m.userTypeIds LIKE %:userTypeIds% AND m.pid IS NULL ORDER BY m.id")
    List<MenuInfo> findByUserTypeIds(@Param("userTypeIds") String userTypeIds);

    // 查询某个父菜单下的子菜单
    @Query("SELECT m FROM MenuInfo m WHERE m.userTypeIds LIKE %:userTypeIds% AND m.pid = :pid ORDER BY m.id")
    List<MenuInfo> findByUserTypeIds(@Param("userTypeIds") String userTypeIds, @Param("pid") Integer pid);

    // 统计某个父菜单下的子菜单数量
    @Query("SELECT COUNT(m) FROM MenuInfo m WHERE m.pid = :pid")
    int countMenuInfoByPid(@Param("pid") Integer pid);
}
