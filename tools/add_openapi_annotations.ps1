$ErrorActionPreference = 'Stop'

$controllerDir = Join-Path $PSScriptRoot '..\src\main\java\cn\edu\sdu\java\server\controllers'

$tagMap = @{
    'AuthController' = @{ name = 'Authentication'; description = 'Login, captcha and register-user APIs' }
    'RegisterController' = @{ name = 'Registration'; description = 'Username validation and registration APIs' }
    'LegacyAuthController' = @{ name = 'Legacy Authentication'; description = 'Legacy login compatibility APIs' }
    'AttendanceController' = @{ name = 'Attendance'; description = 'Attendance record and statistics APIs' }
    'HomeworkController' = @{ name = 'Homework'; description = 'Homework publish, submit, grade and statistics APIs' }
    'CourseSelectionController' = @{ name = 'Course Selection'; description = 'Course selection and withdrawal APIs' }
    'InnovationController' = @{ name = 'Innovation'; description = 'Innovation project APIs' }
    'DevelopmentController' = @{ name = 'Development'; description = 'Development and entrepreneurship APIs' }
    'HonorActivityController' = @{ name = 'Honor And Activity'; description = 'Honor award and daily activity APIs' }
    'StudentController' = @{ name = 'Student'; description = 'Student profile, course material, consumption, score and resume APIs' }
    'CourseController' = @{ name = 'Course'; description = 'Course management APIs' }
    'ExamController' = @{ name = 'Exam'; description = 'Exam schedule and exam student APIs' }
    'StudentLeaveController' = @{ name = 'Student Leave'; description = 'Student leave request and approval APIs' }
    'StudentStatisticsController' = @{ name = 'Student Statistics'; description = 'Student statistics APIs' }
    'StatisticsController' = @{ name = 'Statistics'; description = 'Dashboard statistics APIs' }
    'AvatarController' = @{ name = 'Avatar'; description = 'Avatar upload, download and delete APIs' }
    'PunishmentController' = @{ name = 'Punishment'; description = 'Student punishment management APIs' }
    'LogController' = @{ name = 'Log Monitor'; description = 'Request log, modify log and monitor statistics APIs' }
    'CourseTeachingController' = @{ name = 'Course Teaching'; description = 'Teaching assignment APIs' }
    'SocialPracticeController' = @{ name = 'Social Practice'; description = 'Social practice APIs' }
    'ScoreController' = @{ name = 'Score'; description = 'Score management APIs' }
    'TeacherController' = @{ name = 'Teacher'; description = 'Teacher management APIs' }
    'BaseController' = @{ name = 'Base'; description = 'Base menu, dictionary, file and common APIs' }
    'TestController' = @{ name = 'Test'; description = 'Development and test helper APIs' }
}

$summaryMap = @{
    'authenticateUser' = 'User login'
    'getValidateCode' = 'Get captcha'
    'testValidateInfo' = 'Verify captcha'
    'registerUser' = 'Register user'
    'checkUsername' = 'Check username availability'
    'register' = 'User registration'
    'getDataBaseUserName' = 'Get database username'
    'getTeacherItemOptionList' = 'Get teacher option list'
    'getStudentItemOptionList' = 'Get student option list'
    'getCourseItemOptionList' = 'Get course option list'
    'getYearSemesterOptionList' = 'Get year semester option list'
    'getCourseOptionList' = 'Get course option list'
    'getTeacherOptionList' = 'Get teacher option list'
    'getTypeOptionList' = 'Get type option list'
    'getMainPageData' = 'Get dashboard data'
    'authenticate' = 'Authenticate user'
    'uploadAvatar' = 'Upload avatar'
    'getAvatar' = 'Get avatar'
    'deleteAvatar' = 'Delete avatar'
}

function Split-CamelCase([string]$text) {
    if ([string]::IsNullOrWhiteSpace($text)) {
        return ''
    }
    return (($text -replace '([a-z0-9])([A-Z])', '$1 $2').Trim())
}

function Get-JavaDocSummary($lines, [int]$index) {
    $j = $index - 1
    while ($j -ge 0 -and [string]::IsNullOrWhiteSpace($lines[$j])) {
        $j--
    }
    if ($j -lt 0 -or $lines[$j].Trim() -ne '*/') {
        return $null
    }

    $docLines = New-Object System.Collections.Generic.List[string]
    $j--
    while ($j -ge 0) {
        $trimmed = $lines[$j].Trim()
        if ($trimmed.StartsWith('/**')) {
            break
        }
        $docLines.Insert(0, $trimmed)
        $j--
    }

    foreach ($line in $docLines) {
        $text = ($line -replace '^\*\s?', '').Trim()
        if (-not [string]::IsNullOrWhiteSpace($text) -and -not $text.StartsWith('@')) {
            return $text.TrimEnd('.', ':')
        }
    }
    return $null
}

function Get-MethodName($lines, [int]$index) {
    for ($k = $index; $k -lt [Math]::Min($index + 8, $lines.Count); $k++) {
        if ($lines[$k] -match '\bpublic\b[^(]*\s+([a-zA-Z0-9_]+)\s*\(') {
            return $matches[1]
        }
    }
    return $null
}

function Get-FallbackSummary([string]$methodName) {
    if ([string]::IsNullOrWhiteSpace($methodName)) {
        return 'API operation'
    }
    if ($summaryMap.ContainsKey($methodName)) {
        return $summaryMap[$methodName]
    }

    $patterns = @(
        @{ regex = '^get(.+)List$'; prefix = 'Get '; suffix = ' list' }
        @{ regex = '^get(.+)PageList$'; prefix = 'Get paged '; suffix = ' list' }
        @{ regex = '^get(.+)PageData$'; prefix = 'Get paged '; suffix = ' data' }
        @{ regex = '^get(.+)Info$'; prefix = 'Get '; suffix = ' details' }
        @{ regex = '^get(.+)Statistics$'; prefix = 'Get '; suffix = ' statistics' }
        @{ regex = '^get(.+)Ranking$'; prefix = 'Get '; suffix = ' ranking' }
        @{ regex = '^get(.+)Progress$'; prefix = 'Get '; suffix = ' progress' }
        @{ regex = '^get(.+)$'; prefix = 'Get '; suffix = '' }
        @{ regex = '^save(.+)$'; prefix = 'Save '; suffix = '' }
        @{ regex = '^(.+)Save$'; prefix = 'Save '; suffix = '' }
        @{ regex = '^(.+)Delete$'; prefix = 'Delete '; suffix = '' }
        @{ regex = '^(.+)Approve$'; prefix = 'Approve '; suffix = '' }
        @{ regex = '^(.+)Check$'; prefix = 'Review '; suffix = '' }
        @{ regex = '^check(.+)$'; prefix = 'Check '; suffix = '' }
        @{ regex = '^import(.+)$'; prefix = 'Import '; suffix = '' }
        @{ regex = '^upload(.+)$'; prefix = 'Upload '; suffix = '' }
        @{ regex = '^download(.+)$'; prefix = 'Download '; suffix = '' }
        @{ regex = '^preview(.+)$'; prefix = 'Preview '; suffix = '' }
        @{ regex = '^generate(.+)$'; prefix = 'Generate '; suffix = '' }
        @{ regex = '^select(.+)$'; prefix = 'Select '; suffix = '' }
        @{ regex = '^drop(.+)$'; prefix = 'Drop '; suffix = '' }
        @{ regex = '^batch(.+)$'; prefix = 'Batch process '; suffix = '' }
        @{ regex = '^sync(.+)$'; prefix = 'Sync '; suffix = '' }
        @{ regex = '^do(.+)$'; prefix = 'Execute '; suffix = '' }
    )

    foreach ($pattern in $patterns) {
        if ($methodName -match $pattern.regex) {
            $target = Split-CamelCase $matches[1]
            return ($pattern.prefix + $target + $pattern.suffix).Trim()
        }
    }

    return (Split-CamelCase $methodName)
}

Get-ChildItem -Path $controllerDir -Filter '*Controller.java' | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content -Path $file -Raw -Encoding UTF8
    $lines = [System.Collections.Generic.List[string]]::new()
    foreach ($line in ($content -split "`r?`n")) {
        [void]$lines.Add($line)
    }

    $className = $_.BaseName
    $tag = $tagMap[$className]
    if (-not $tag) {
        $tag = @{ name = $className; description = "$className APIs" }
    }

    if ($content -notmatch 'import io\.swagger\.v3\.oas\.annotations\.Operation;') {
        $insertIndex = 1
        while ($insertIndex -lt $lines.Count -and -not $lines[$insertIndex].StartsWith('import ')) {
            $insertIndex++
        }
        $lines.Insert($insertIndex, 'import io.swagger.v3.oas.annotations.tags.Tag;')
        $lines.Insert($insertIndex, 'import io.swagger.v3.oas.annotations.Operation;')
    }

    $classIndex = -1
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match ('public class ' + [regex]::Escape($className) + '\b')) {
            $classIndex = $i
            break
        }
    }

    if ($classIndex -ge 0 -and ($content -notmatch '@Tag\(')) {
        $lines.Insert($classIndex, '@Tag(name = "' + $tag.name + '", description = "' + $tag.description + '")')
        $classIndex++
    }

    $classDeclared = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match ('public class ' + [regex]::Escape($className) + '\b')) {
            $classDeclared = $true
            continue
        }

        if (-not $classDeclared) {
            continue
        }

        if ($lines[$i].Trim() -match '^@(PostMapping|GetMapping|DeleteMapping|PutMapping|PatchMapping)\b') {
            $prev = $i - 1
            while ($prev -ge 0 -and [string]::IsNullOrWhiteSpace($lines[$prev])) {
                $prev--
            }
            if ($prev -ge 0 -and $lines[$prev].Trim().StartsWith('@Operation(')) {
                continue
            }

            $summary = Get-JavaDocSummary $lines $i
            if (-not $summary) {
                $methodName = Get-MethodName $lines $i
                $summary = Get-FallbackSummary $methodName
            }

            $indent = ''
            if ($lines[$i] -match '^(\s*)@') {
                $indent = $matches[1]
            }
            $lines.Insert($i, $indent + '@Operation(summary = "' + ($summary -replace '"', '\"') + '")')
            $i++
        }
    }

    $updated = [string]::Join("`r`n", $lines)
    if (-not $updated.EndsWith("`r`n")) {
        $updated += "`r`n"
    }
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($file, $updated, $utf8NoBom)
}
