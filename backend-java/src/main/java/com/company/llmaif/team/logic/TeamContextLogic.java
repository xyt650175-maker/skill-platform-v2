package com.company.llmaif.team.logic;

import com.company.llmaif.common.AgentException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** 从登录用户的团队成员关系解析当前团队，禁止用用户 ID 冒充团队 ID。 */
@Component
@RequiredArgsConstructor
public class TeamContextLogic {
    private final JdbcTemplate jdbcTemplate;

    public Long resolveTeamId(Long userId, String requestedTeamId) {
        List<Long> teamIds = jdbcTemplate.queryForList(
                "SELECT team_id FROM team_member WHERE user_id = ? ORDER BY team_id", Long.class, userId);
        if (teamIds.isEmpty()) throw new AgentException("当前用户未加入团队，无法访问团队级 Skill 与 Git 凭证");
        if (requestedTeamId == null || requestedTeamId.trim().isEmpty()) return teamIds.get(0);
        try {
            Long teamId = Long.valueOf(requestedTeamId.trim());
            if (!teamIds.contains(teamId)) throw new AgentException("无权访问指定团队");
            return teamId;
        } catch (NumberFormatException e) {
            throw new AgentException("团队标识不合法");
        }
    }
}
